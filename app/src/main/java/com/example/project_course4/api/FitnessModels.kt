package com.example.project_course4.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Muscles ─────────────────────────────────────────────────────────────────

@Serializable
data class MuscleResponse(
    val id: Int,
    @SerialName("muscle_name") val name: String,
    @SerialName("muscle_group_id") val groupId: Int,
    @SerialName("group_name") val groupName: String? = null
)

// ─── Exercises ───────────────────────────────────────────────────────────────

@Serializable
data class ExerciseResponse(
    val id: Int,
    @SerialName("exercise_name") val name: String,
    @SerialName("exercise_name_ru") val nameRu: String? = null,
    val equipment: String? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
    val level: String? = null,
    val category: String? = null,
    @SerialName("force_type") val forceType: String? = null,
    val mechanic: String? = null,
    @SerialName("target_muscle_id") val targetMuscleId: Int,
    @SerialName("target_muscle_name") val targetMuscleName: String? = null,
    @SerialName("secondary_muscles") val secondaryMuscles: List<MuscleResponse> = emptyList(),
    val instructions: List<ExerciseInstructionResponse> = emptyList()
)

@Serializable
data class ExerciseInstructionResponse(
    val id: Int = 0,
    @SerialName("step_order") val stepOrder: Int,
    @SerialName("instruction_ru") val instructionRu: String? = null,
    val instruction: String
)

@Serializable
data class ExerciseListResponse(
    val exercises: List<ExerciseResponse> = emptyList(),
    val total: Int = 0
)

// ─── Training (workout log) ───────────────────────────────────────────────────

// Detailed set data — declared first so TrainingExerciseData can reference it
@Serializable
data class TrainingSetData(
    @SerialName("set_number") val setNumber: Int,
    @SerialName("weight_kg") val weightKg: Float? = null,
    val reps: Int? = null,
    @SerialName("duration_sec") val durationSec: Int? = null
)

@Serializable
data class TrainingExerciseData(
    @SerialName("exercise_id") val exerciseId: Int,
    @SerialName("exercise_name_ru") val exerciseNameRu: String? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Float? = null,
    @SerialName("exercise_time") val exerciseTime: Int? = null,  // seconds
    @SerialName("detailed_sets") val detailedSets: List<TrainingSetData> = emptyList()
)

@Serializable
data class TrainingData(
    val id: Int = 0,
    @SerialName("training_name") val name: String,
    @SerialName("training_date") val date: String? = null,
    @SerialName("training_description") val description: String? = null,
    val notes: String? = null,
    val exercises: List<TrainingExerciseData> = emptyList()
)

@Serializable
data class TrainingListResponse(
    val trainings: List<TrainingData> = emptyList()
)

@Serializable
data class TrainingSaveRequest(
    @SerialName("training_name") val name: String,
    @SerialName("training_date") val date: String,
    @SerialName("training_description") val description: String? = null,
    val notes: String? = null,
    val exercises: List<TrainingExerciseData>
)

@Serializable
data class TrainingSaveResponse(
    val success: Boolean,
    val id: Int = 0,
    val message: String? = null
)

// ─── Training with sets (detailed) ───────────────────────────────────────────

@Serializable
data class TrainingExerciseWithSets(
    @SerialName("exercise_id") val exerciseId: Int,
    val sets: List<TrainingSetData> = emptyList()
)

@Serializable
data class TrainingWithSetsSaveRequest(
    @SerialName("training_name") val name: String,
    @SerialName("training_date") val date: String,
    @SerialName("training_description") val description: String? = null,
    val notes: String? = null,
    val exercises: List<TrainingExerciseWithSets>
)

// ─── Training Plan ────────────────────────────────────────────────────────────

@Serializable
data class TrainingPlanExerciseData(
    @SerialName("exercise_id") val exerciseId: Int,
    @SerialName("exercise_name_ru") val exerciseNameRu: String? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Float? = null,
    @SerialName("exercise_time") val exerciseTime: Int? = null
)

@Serializable
data class TrainingPlanDayData(
    val id: Int = 0,
    @SerialName("day_number") val dayNumber: Int,
    @SerialName("day_name") val dayName: String? = null,
    val notes: String? = null,
    val exercises: List<TrainingPlanExerciseData> = emptyList()
)

@Serializable
data class TrainingPlanData(
    val id: Int = 0,
    @SerialName("plan_name") val name: String,
    @SerialName("plan_description") val description: String? = null,
    @SerialName("is_public") val isPublicRaw: Int = 0,
    @SerialName("creator_id") val creatorId: Int = 0,
    val days: List<TrainingPlanDayData> = emptyList(),
    @SerialName("day_count") val dayCount: Int? = null,
    @SerialName("assigned_by_trainer_id") val assignedByTrainerId: Int? = null
) {
    val isPublic: Boolean get() = isPublicRaw != 0
    val isAssignedByTrainer: Boolean get() = assignedByTrainerId != null
}

@Serializable
data class TrainingPlanListResponse(
    val plans: List<TrainingPlanData> = emptyList()
)

@Serializable
data class TrainingPlanSaveRequest(
    @SerialName("plan_name") val name: String,
    @SerialName("plan_description") val description: String = "",
    @SerialName("is_public") val isPublic: Boolean = false,
    val days: List<TrainingPlanDayData>
)

@Serializable
data class TrainingPlanVisibilityRequest(
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class TrainingPlanSaveResponse(
    val success: Boolean,
    val id: Int = 0,
    val message: String? = null
)
