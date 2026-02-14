package com.example.project_course4.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import com.example.project_course4.local_db.entities.Meal
import com.example.project_course4.local_db.entities.MealComponent
import com.example.project_course4.local_db.entities.MealMealComponent

@Dao
interface MealDao {

    @Insert
    suspend fun insertMeal(meal: Meal): Long

    @Insert
    suspend fun insertComponents(component: List<MealComponent>): List<Long>

    @Insert
    suspend fun insertRelations(relations: List<MealMealComponent>)

    // Транзакция для сохранения всего приема пищи целиком
    @Transaction
    suspend fun insertFullMeal(meal: Meal, components: List<Pair<Int, UShort>>) {
        val mealId = insertMeal(meal) // Вставка приема пищи для получения его ID

        val componentEntities = components.map {
            MealComponent(mealId.toInt(), it.first, it.second)
        }
        val componentIds = insertComponents(componentEntities) // Вставка компонентов

        val relations = componentIds.map { componentId ->
            MealMealComponent(mealId = mealId.toInt(), mealComponentId = componentId.toInt())
        }
        insertRelations(relations) // Вставка связей между приёмом пищи и компонентами
    }

}