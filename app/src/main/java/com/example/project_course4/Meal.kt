package com.example.project_course4

import java.time.LocalTime

data class Meal(
    val id: String,
    val time: LocalTime,
    val name: String
) //TODO потом удалить этот класс, т.к всё будет храниться в БД