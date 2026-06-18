package com.diatoms.mystreamdeck.model

data class MacroButton(
    val id: Int,
    val label: String,
    val subLabel: String? = null,
    val colorHex: Long = 0xFF1E2A3A,
    val apiUrl: String = "",
)

val defaultButtons: List<MacroButton> = (1..15).map { i ->
    if (i == 1) MacroButton(
        id = 1,
        label = "YT Workspace",
        subLabel = "Open on Display 2",
        colorHex = 0xFF1A4A7A,
        apiUrl = "http://10.158.12.5:8765/button/1"
    )
    else MacroButton(id = i, label = "#$i")
}
