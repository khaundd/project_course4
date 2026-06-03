package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "active_workout")
data class ActiveWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startedAt: Long,              // epoch millis
    val finishedAt: Long? = null,     // null = ещё идёт
    val notes: String = "",
    val sourceTrainingId: Int? = null, // если запущена из шаблона
    val pausedElapsedSec: Long = 0L   // накопленное время до последней паузы (сек)
)

@Entity(
    tableName = "active_workout_set",
    foreignKeys = [ForeignKey(
        entity = ActiveWorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class ActiveWorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutId: Int,
    val exerciseId: Int,
    val exerciseNameRu: String,
    val setNumber: Int,
    val weightKg: Float? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restTimeSec: Int = 90,
    val isSkipped: Boolean = false,
    val completedAt: Long? = null  // null = ещё не выполнен
)
