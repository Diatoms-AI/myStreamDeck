package com.diatoms.mystreamdeck.model

data class MacroButton(
    val id: Int,
    val label: String,
    val subLabel: String? = null,
    val colorHex: Long = 0xFF1E2A3A,
)

val defaultButtons: List<MacroButton> = listOf(
    MacroButton(id = 1,  label = "Launch",  subLabel = "API #1",  colorHex = 0xFF1A4A7A),
    MacroButton(id = 2,  label = "#2",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 3,  label = "#3",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 4,  label = "#4",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 5,  label = "#5",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 6,  label = "#6",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 7,  label = "#7",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 8,  label = "#8",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 9,  label = "#9",      colorHex = 0xFF1E2A3A),
    MacroButton(id = 10, label = "#10",     colorHex = 0xFF1E2A3A),
    MacroButton(id = 11, label = "#11",     colorHex = 0xFF1E2A3A),
    MacroButton(id = 12, label = "#12",     colorHex = 0xFF1E2A3A),
    MacroButton(id = 13, label = "#13",     colorHex = 0xFF1E2A3A),
    MacroButton(id = 14, label = "#14",     colorHex = 0xFF1E2A3A),
    MacroButton(id = 15, label = "#15",     colorHex = 0xFF1E2A3A),
)
