package com.rivo.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ApiConfigEntity::class, SystemPromptEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun systemPromptDao(): SystemPromptDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
