package com.homework.assistant.model

data class CourseItem(
    val courseId: String,
    val classId: String,
    val cpi: String,
    val courseName: String,
    val teacher: String = ""
)
