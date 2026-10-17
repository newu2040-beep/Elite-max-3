package com.example.core.storage

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

enum class MediaOperationType(val displayLabel: String, val isVideo: Boolean) {
    VIDEO_ENHANCE("Video Enhancer", true),
    PHOTO_ENHANCE("Photo Enhancer", false),
    VIDEO_COMPRESS("Video Compressor", true),
    PHOTO_COMPRESS("Photo Compressor", false)
}

enum class JobStatus(val label: String) {
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELED("Canceled")
}

@Entity(tableName = "media_jobs")
data class MediaJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val filename: String,
    val sourceUri: String,
    val outputUri: String,
    val sourceSizeBytes: Long,
    val outputSizeBytes: Long,
    val sourceDimensions: String,
    val outputDimensions: String,
    val durationMs: Long = 0L,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val settingsSummary: String = "",
    val errorCode: String? = null
) {
    val savingsPercent: Int?
        get() {
            if (sourceSizeBytes <= 0L || outputSizeBytes <= 0L) return null
            val diff = sourceSizeBytes - outputSizeBytes
            return ((diff.toDouble() / sourceSizeBytes.toDouble()) * 100.0).toInt()
        }
}

@Dao
interface MediaJobDao {
    @Query("SELECT * FROM media_jobs ORDER BY completedAt DESC")
    fun getAllJobs(): Flow<List<MediaJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: MediaJobEntity): Long

    @Query("DELETE FROM media_jobs WHERE id = :id")
    suspend fun deleteJobById(id: Long)

    @Query("DELETE FROM media_jobs")
    suspend fun clearAllJobs()
}

@Database(entities = [MediaJobEntity::class], version = 1, exportSchema = false)
abstract class EliteDatabase : RoomDatabase() {
    abstract fun mediaJobDao(): MediaJobDao

    companion object {
        @Volatile
        private var INSTANCE: EliteDatabase? = null

        fun getInstance(context: Context): EliteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EliteDatabase::class.java,
                    "elitemax_history.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class MediaJobRepository(private val dao: MediaJobDao) {
    val allJobs: Flow<List<MediaJobEntity>> = dao.getAllJobs()

    suspend fun insertJob(job: MediaJobEntity): Long = dao.insertJob(job)

    suspend fun deleteJobById(id: Long) = dao.deleteJobById(id)

    suspend fun clearAllJobs() = dao.clearAllJobs()
}
