package com.nocta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT conversationId FROM ai_messages ORDER BY createdAtEpochMillis DESC LIMIT 1")
    fun observeMostRecentConversationId(): Flow<String?>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis ASC")
    fun observeConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Insert
    suspend fun insert(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages")
    suspend fun deleteAll()
}
