package com.jotapem.dlmstranslator.services

data class TranslationHistoryEntry(
    val id: String,
    val timestampMillis: Long,
    val input: String,
    val inputType: DlmsTranslatorService.InputType,
    val output: String
)
