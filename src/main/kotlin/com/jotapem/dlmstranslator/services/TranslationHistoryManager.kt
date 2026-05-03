package com.jotapem.dlmstranslator.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class TranslationHistoryManager {

    private val entries: MutableList<TranslationHistoryEntry> = mutableListOf()

    fun addEntry(entry: TranslationHistoryEntry) {
        entries.add(0, entry)
    }

    fun getEntries(): List<TranslationHistoryEntry> = entries.toList()

    fun removeEntry(id: String) {
        entries.removeIf { it.id == id }
    }

    fun clearAll() {
        entries.clear()
    }
}
