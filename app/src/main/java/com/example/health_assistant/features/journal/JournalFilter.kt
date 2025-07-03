package com.example.health_assistant.features.journal

import java.util.Date

sealed class JournalFilter {
    object All : JournalFilter()
    data class ByType(val type: String) : JournalFilter()
    data class ByTypes(val types: List<String>) : JournalFilter()
    data class ByDate(val date: Date) : JournalFilter()
    data class ByDateRange(val from: Date, val to: Date) : JournalFilter()
    data class Search(val query: String) : JournalFilter()
}
