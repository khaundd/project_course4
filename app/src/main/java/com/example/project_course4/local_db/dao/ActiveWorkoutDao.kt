package com.example.project_course4.local_db.dao

import androidx.room.*
import com.example.project_course4.local_db.entities.ActiveWorkoutEntity
import com.example.project_course4.local_db.entities.ActiveWorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveWorkoutDao {

    // ─── Workout ──────────────────────────────────────────────────────────────

    @Insert
    suspend fun insertWorkout(workout: ActiveWorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: ActiveWorkoutEntity)

    @Query("SELECT * FROM active_workout WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActiveWorkout(): ActiveWorkoutEntity?

    @Query("SELECT * FROM active_workout WHERE id = :id")
    suspend fun getWorkoutById(id: Int): ActiveWorkoutEntity?

    @Query("DELETE FROM active_workout WHERE id = :id")
    suspend fun deleteWorkout(id: Int)

    // ─── Sets ─────────────────────────────────────────────────────────────────

    @Insert
    suspend fun insertSet(set: ActiveWorkoutSetEntity): Long

    @Update
    suspend fun updateSet(set: ActiveWorkoutSetEntity)

    @Query("SELECT * FROM active_workout_set WHERE workoutId = :workoutId ORDER BY exerciseId, setNumber")
    fun getSetsForWorkout(workoutId: Int): Flow<List<ActiveWorkoutSetEntity>>

    @Query("SELECT * FROM active_workout_set WHERE workoutId = :workoutId ORDER BY exerciseId, setNumber")
    suspend fun getSetsForWorkoutOnce(workoutId: Int): List<ActiveWorkoutSetEntity>

    @Query("DELETE FROM active_workout_set WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExercise(workoutId: Int, exerciseId: Int)
}
