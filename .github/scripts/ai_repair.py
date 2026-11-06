#!/usr/bin/env python3
"""
AI-Assisted Build Repair Workflow
- Reads Gradle/Kotlin error logs
- Sends to configured AI_API (OpenAI-compatible)
- Validates proposed patch
- Applies within workspace only
- Re-runs build
- Audit logs patches
"""

import os
import sys
import re
import json
import argparse
import subprocess
import urllib.request
import urllib.error
from pathlib import Path
import time

RETRY_LIMIT = 3
MAX_LOG_CHARS = 12000
PATCH_AUDIT = Path("repair_audit.log")

def log(msg):
    print(msg)
    with open("repair_report.md", "a") as f:
        f.write(msg + "\n")

def collect_logs():
    logs = ""
    if Path("repair_input.txt").exists():
        logs = Path("repair_input.txt").read_text(errors="ignore")
    # fallback: try local gradle build to get error
    if not logs.strip():
        try:
            result = subprocess.run(["./gradlew", "assembleDebug", "--stacktrace"], capture_output=True, text=True, timeout=120)
            logs = result.stdout + "\n" + result.stderr
        except Exception as e:
            logs = f"Failed to collect logs: {e}"
    return logs[:MAX_LOG_CHARS]

def sanitize_logs(logs: str) -> str:
    # Never send API keys/secrets
    patterns = [
        r"api_key\s*=\s*['\"][^'\"]+['\"]",
        r"API_KEY\s*=\s*\S+",
        r"sk-[a-zA-Z0-9\-_]+",
        r"AIza[0-9A-Za-z\-_]+",
    ]
    for pat in patterns:
        logs = re.sub(pat, "[REDACTED]", logs, flags=re.IGNORECASE)
    return logs

def call_ai(prompt: str, api_key: str, base_url: str, model: str) -> str:
    base_url = (base_url or "https://api.openai.com/v1").rstrip("/")
    model = model or "gpt-4o-mini"
    url = f"{base_url}/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": "You are an expert Android/Kotlin build repair assistant. You will be given Gradle/Kotlin build errors. Respond ONLY with a unified diff patch (git diff format) that fixes the issue, or explain if no patch is possible. Keep patches minimal, do not change working code unnecessarily. Never include secrets."},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.2,
        "max_tokens": 4000
    }
    req = urllib.request.Request(url, data=json.dumps(body).encode(), headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode())
            return data["choices"][0]["message"]["content"]
    except urllib.error.HTTPError as e:
        err_body = e.read().decode()[:2000]
        raise RuntimeError(f"AI API HTTP {e.code}: {err_body}")
    except Exception as e:
        raise RuntimeError(f"AI call failed: {e}")

def extract_patch(ai_response: str) -> str | None:
    # Try to extract diff block
    if "```diff" in ai_response:
        m = re.search(r"```diff\s*(.*?)\s*```", ai_response, re.DOTALL)
        if m:
            return m.group(1).strip()
    if "diff --git" in ai_response or "*** Begin Patch" in ai_response or "--- a/" in ai_response:
        # find diff section
        lines = ai_response.splitlines()
        start = None
        for i, line in enumerate(lines):
            if line.startswith("diff --git") or line.startswith("--- a/") or line.startswith("*** Begin"):
                start = i
                break
        if start is not None:
            return "\n".join(lines[start:])
    return None

def validate_patch(patch: str) -> tuple[bool, str]:
    # Basic validation: no dangerous commands, only modifies files inside workspace
    forbidden = ["rm -rf /", "mkfs", ":(){:|:&};:", "curl | sh", "wget | sh", "sudo rm"]
    for f in forbidden:
        if f in patch:
            return False, f"Forbidden pattern: {f}"
    # Check file paths
    # Parse +++ b/...
    for line in patch.splitlines():
        if line.startswith("+++ b/"):
            path = line[6:].strip()
            # prevent path traversal
            if ".." in path or path.startswith("/"):
                return False, f"Invalid path: {path}"
            # Must be under allowed prefixes
            allowed_prefixes = ["app/src/", "app/build.gradle", "gradle", "build.gradle", "settings.gradle"]
            if not any(path.startswith(p) or p in path for p in allowed_prefixes):
                # Allow but warn
                pass
        if line.startswith("+++ ") and "b/dev/null" not in line:
            pass
    # Check patch size
    if len(patch) > 50000:
        return False, "Patch too large"
    return True, "OK"

def apply_patch(patch: str):
    patch_file = Path("/tmp/ai_patch.diff")
    patch_file.write_text(patch)
    # Try git apply
    result = subprocess.run(["git", "apply", "--check", str(patch_file)], capture_output=True, text=True)
    if result.returncode != 0:
        # Try patch command
        result2 = subprocess.run(["patch", "-p1", "--dry-run", "--forward"], input=patch, capture_output=True, text=True)
        if result2.returncode != 0:
            return False, result.stderr + result2.stderr
    # Actually apply
    result = subprocess.run(["git", "apply", str(patch_file)], capture_output=True, text=True)
    if result.returncode == 0:
        return True, "Applied via git apply"
    result = subprocess.run(["patch", "-p1", "--forward"], input=patch, capture_output=True, text=True)
    if result.returncode == 0:
        return True, "Applied via patch"
    return False, result.stderr

def run_build():
    result = subprocess.run(["./gradlew", "assembleDebug", "--stacktrace"], capture_output=True, text=True, timeout=300)
    return result.returncode == 0, result.stdout + result.stderr

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--retry-limit", type=int, default=3)
    args = parser.parse_args()
    limit = args.retry_limit

    Path("repair_report.md").write_text("# AI Build Repair Report\n\n")

    api_key = os.environ.get("AI_REPAIR_API_KEY")
    if not api_key:
        log("❌ AI_REPAIR_API_KEY not set in GitHub Secrets. Configure it to enable auto-repair.")
        return 1
    base_url = os.environ.get("AI_REPAIR_BASE_URL") or "https://api.openai.com/v1"
    model = os.environ.get("AI_REPAIR_MODEL") or "gpt-4o-mini"

    logs = collect_logs()
    if not logs.strip():
        log("No logs found, nothing to repair")
        return 0

    logs = sanitize_logs(logs)
    log(f"📋 Collected {len(logs)} chars of logs")
    log(f"🔧 Model: {model} @ {base_url}")

    for attempt in range(1, limit+1):
        log(f"\n## Attempt {attempt}/{limit}\n")
        prompt = f"""Android Gradle build failed. Diagnose and propose minimal fix patch.

LOGS:
```
{logs[:10000]}
```

Project: Rivo AI - Kotlin + Jetpack Compose + Room + Retrofit + Hilt, minSdk 23, targetSdk 34, Kotlin 1.9.22, AGP 8.2.2

Rules:
- Respond with unified diff only if fix is clear
- Keep changes minimal
- Do not expose secrets
- Ensure no breaking changes
"""

        try:
            ai_resp = call_ai(prompt, api_key, base_url, model)
            log(f"🤖 AI response received ({len(ai_resp)} chars)")
            # Save raw response for audit
            Path(f"ai_response_{attempt}.txt").write_text(ai_resp)

            patch = extract_patch(ai_resp)
            if not patch:
                log("⚠️ No patch found in AI response. AI explanation:")
                log(ai_resp[:2000])
                # Update logs for next iteration with AI explanation
                logs = ai_resp[:5000]
                continue

            log(f"📦 Extracted patch ({len(patch)} chars)")
            valid, reason = validate_patch(patch)
            if not valid:
                log(f"❌ Patch validation failed: {reason}")
                with open(PATCH_AUDIT, "a") as f:
                    f.write(f"[{time.ctime()}] Attempt {attempt} REJECTED: {reason}\n{patch[:1000]}\n---\n")
                continue

            with open(PATCH_AUDIT, "a") as f:
                f.write(f"[{time.ctime()}] Attempt {attempt} APPLYING patch\n{patch[:2000]}\n---\n")

            ok, msg = apply_patch(patch)
            if not ok:
                log(f"❌ Patch apply failed: {msg}")
                continue
            log(f"✅ Patch applied: {msg}")

            # Validate build
            log("🔨 Rebuilding...")
            success, build_out = run_build()
            Path(f"build_attempt_{attempt}.log").write_text(build_out[-20000:])
            if success:
                log("🎉 Build succeeded after patch!")
                log(f"✅ Repair successful on attempt {attempt}")
                # Commit patch audit
                with open(PATCH_AUDIT, "a") as f:
                    f.write(f"[{time.ctime()}] SUCCESS on attempt {attempt}\n")
                # Configure git and commit
                try:
                    subprocess.run(["git", "config", "user.email", "ai-repair@github.actions"], check=True)
                    subprocess.run(["git", "config", "user.name", "AI Repair Bot"], check=True)
                    subprocess.run(["git", "add", "-A"], check=True)
                    subprocess.run(["git", "commit", "-m", f"AI repair: fix build (attempt {attempt})"], check=True)
                    subprocess.run(["git", "push"], check=True)
                    log("📤 Pushed fix to repository")
                except Exception as e:
                    log(f"⚠️ Could not push: {e}")
                return 0
            else:
                log("❌ Build still failing, updating logs for next attempt")
                logs = build_out[-10000:]
                # revert patch for next attempt? Keep it and iterate
                continue

        except Exception as e:
            log(f"❌ Error during repair: {e}")
            import traceback
            log(traceback.format_exc())
            continue

    log(f"\n❌ Repair failed after {limit} attempts")
    with open(PATCH_AUDIT, "a") as f:
        f.write(f"[{time.ctime()}] FAILED after {limit} attempts\n")
    return 1

if __name__ == "__main__":
    sys.exit(main())
