package com.example.project_course4.local_db.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.local_db.entities.MealComponent
import com.example.project_course4.local_db.entities.MealMealComponent
import com.example.project_course4.local_db.MealComponentWithJunction

@Dao
interface MealDao {

    @Insert
    suspend fun insertMeal(mealEntity: MealEntity): Long

    @Transaction
    suspend fun insertMealOnly(mealEntity: MealEntity): Int {
        return insertMeal(mealEntity).toInt()
    }

    @Insert
    suspend fun insertComponents(component: List<MealComponent>): List<Long>

    @Insert
    suspend fun insertRelations(relations: List<MealMealComponent>)

    @Query("DELETE FROM meal WHERE mealId = :mealId")
    suspend fun deleteMealById(mealId: Int)

    // Транзакция для сохранения всего приема пищи целиком
    @Transaction
    suspend fun insertFullMeal(mealEntity: MealEntity, components: List<Pair<Int, UShort>>): Pair<Int, List<Int>> {
        val mealId = insertMeal(mealEntity).toInt()
        val junctionIds = mutableListOf<Int>()

        components.forEach { (prodId, prodWeight) ->
            val junctionId = insertSingleRelation(MealMealComponent(mealId = mealId)).toInt()
            junctionIds.add(junctionId) // Собираем ID каждой связи

            val component = MealComponent(
                mealMealComponentId = junctionId,
                productId = prodId,
                weight = prodWeight
            )
            insertSingleComponent(component)
        }
        return Pair(mealId, junctionIds) // Возвращаем ID приема и список ID связей
    }
    @Transaction
    suspend fun addComponentsToMeal(mealId: Int, components: List<Pair<Int, UShort>>): List<Int> {
        val junctionIds = mutableListOf<Int>()
        components.forEach { (prodId, prodWeight) ->
            val junctionId = insertSingleRelation(MealMealComponent(mealId = mealId)).toInt()
            junctionIds.add(junctionId)

            insertSingleComponent(MealComponent(
                mealMealComponentId = junctionId,
                productId = prodId,
                weight = prodWeight
            ))
        }
        return junctionIds
    }

    @Insert
    suspend fun insertSingleRelation(relation: MealMealComponent): Long

    @Insert
    suspend fun insertSingleComponent(component: MealComponent): Long

    @Query("DELETE FROM meal_meal_component WHERE id = :junctionId")
    suspend fun deleteJunctionById(junctionId: Int)

    @Query("DELETE FROM meal_meal_component WHERE mealId = :mId AND id = :junctionId")
    suspend fun deleteProductLink(mId: Int, junctionId: Int)

    @Query("DELETE FROM meal")
    suspend fun clearAllMeals()

    @Transaction
    suspend fun fullResetMeals() {
        Log.d("MealDao", "Начало полного сброса приёмов пищи")
        clearAllMeals()
        resetMealCounter()
        Log.d("MealDao", "Полный сброс приёмов пищи завершён")
    }

    @Query("DELETE FROM sqlite_sequence WHERE name = 'meal'")
    suspend fun resetMealCounter()

    @Query("UPDATE meal_component SET weight = :newWeight WHERE mealMealComponentId = :junctionId")
    suspend fun updateWeight(junctionId: Int, newWeight: UShort)

    @Query("SELECT * FROM meal ORDER BY mealTime")
    suspend fun getAllMeals(): List<MealEntity>

    @Query("SELECT * FROM meal WHERE mealDate = :date ORDER BY mealTime")
    suspend fun getMealsByDate(date: Long): List<MealEntity>

    @Query("UPDATE meal SET mealTime = :newTime WHERE mealId = :mealId")
    suspend fun updateMealTime(mealId: Int, newTime: Long)

    @Query("""
        SELECT mmc.mealId AS mealId, mmc.id AS junctionId, mc.productId AS productId, mc.weight AS weight
        FROM meal_component mc
        INNER JOIN meal_meal_component mmc ON mc.mealMealComponentId = mmc.id
        ORDER BY mmc.mealId
    """)
    suspend fun getAllMealComponentsWithJunction(): List<MealComponentWithJunction>
}