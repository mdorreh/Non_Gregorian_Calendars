package com.example.non_gregoriancalendars

data class Calendar(
    val id: Int,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val accessLevel: Int
) {
    fun canWrite() = accessLevel >= 500

    fun getFullTitle() = "$displayName ($accountName)"
}