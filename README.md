# Rivo AI — Premium Android AI Chat

Premium native Android AI chat application built with Kotlin, Jetpack Compose, Room, and Retrofit.

## Features

- **AI Chat** with streaming, markdown, code blocks, copy, stop generation
- **Multiple API Configurations**: Gemini, OpenAI, Custom OpenAI-compatible
- **Automatic Model Discovery** (real API endpoints, no fake lists)
- **Multiple System Prompts per API**
- **Persistent Chat History** (Room)
- **Secure API key storage** (EncryptedSharedPreferences + Android Keystore)
- **Premium UI**: Soft White, Lavender, Purple/Pink gradients, glowing orb, glassmorphism

## Tech Stack

- Kotlin 1.9.22, Jetpack Compose, MVVM, Hilt, Room, DataStore, Retrofit + OkHttp, Coroutines/Flow
- Min SDK 23, Target 34

## Quick Start

1. Open in Android Studio Hedgehog+
2. Sync Gradle
3. Run on device/emulator (Android 6.0+)

Add your API configuration in **API Settings** → Fetch Models → Set Active.

## Build

```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

- **Build APK** on push to `main`/`arena/**` — uploads `rivo-ai-debug-apk` artifact
- **AI Repair**: On build failure, runs `ai_repair.py` using `AI_REPAIR_API_KEY` secret (OpenAI-compatible)

### Required Secrets

- `AI_REPAIR_API_KEY` — API key for repair LLM
- `AI_REPAIR_BASE_URL` (optional) — e.g. `https://api.openai.com/v1`
- `AI_REPAIR_MODEL` (optional) — e.g. `gpt-4o-mini`

Without the secret, builds fail normally with clear message.

## Security

- API keys never logged, never in git, stored encrypted
- HTTPS only, URL validation

## License

MIT
