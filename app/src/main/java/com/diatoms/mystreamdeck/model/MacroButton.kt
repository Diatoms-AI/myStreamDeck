package com.diatoms.mystreamdeck.model

data class MacroButton(
    val id: Int,
    val label: String,
    val subLabel: String? = null,
    val colorHex: Long = 0xFF1E2A3A,
    val apiUrl: String = "",
)

val defaultButtons: List<MacroButton> = (1..15).map { i ->
    if (i == 1) MacroButton(id = 1, label = "Launch", subLabel = "API #1", colorHex = 0xFF1A4A7A)
    else MacroButton(id = i, label = "#$i")
}
