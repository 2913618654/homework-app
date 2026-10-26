package com.homework.assistant.model

data class HomeworkItem(
    val id: String,
    val platform: String,
    val courseName: String,
    val title: String,
    val status: String,
    val deadline: String,
    val url: String = "",
    var isCompleted: Boolean = false,
    var isExpired: Boolean = false,
    var isUrgent: Boolean = false,
    var remainingText: String = "",
    var sortKey: Long = Long.MAX_VALUE
)
