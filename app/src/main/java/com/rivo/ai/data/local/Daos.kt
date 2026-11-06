package com.rivo.ai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {
    @Query("SELECT * FROM api_configs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ApiConfigEntity>

    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getById(id: Long): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ApiConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiConfigEntity): Long

    @Update
    suspend fun update(entity: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE api_configs SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE api_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)
}

@Dao
interface SystemPromptDao {
    @Query("SELECT * FROM system_prompts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SystemPromptEntity>>

    @Query("SELECT * FROM system_prompts WHERE apiConfigId IS NULL OR apiConfigId = :apiConfigId ORDER BY createdAt DESC")
    fun observeForApi(apiConfigId: Long): Flow<List<SystemPromptEntity>>

    @Query("SELECT * FROM system_prompts ORDER BY createdAt DESC")
    suspend fun getAll(): List<SystemPromptEntity>

    @Query("SELECT * FROM system_prompts WHERE id = :id")
    suspend fun getById(id: Long): SystemPromptEntity?

    @Query("SELECT * FROM system_prompts WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): SystemPromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SystemPromptEntity): Long

    @Update
    suspend fun update(entity: SystemPromptEntity)

    @Query("DELETE FROM system_prompts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE system_prompts SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE system_prompts SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity): Long

    @Update
    suspend fun update(entity: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    suspend fun getForConversation(conversationId: Long): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MessageEntity>)

    @Update
    suspend fun update(entity: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?
}
