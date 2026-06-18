package com.diatoms.mystreamdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.diatoms.mystreamdeck.model.MacroButton
import com.diatoms.mystreamdeck.model.defaultButtons
import com.diatoms.mystreamdeck.ui.MacroConfigScreen
import com.diatoms.mystreamdeck.ui.MainScreen
import com.diatoms.mystreamdeck.ui.theme.MyStreamDeckTheme

private sealed class Screen {
    object Deck : Screen()
    object Config : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyStreamDeckTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Deck) }
                var buttons by remember { mutableStateOf(defaultButtons) }

                when (screen) {
                    Screen.Deck -> MainScreen(
                        buttons = buttons,
                        onSettingsClick = { screen = Screen.Config }
                    )
                    Screen.Config -> MacroConfigScreen(
                        buttons = buttons,
                        onUpdate = { updated ->
                            buttons = buttons.map { if (it.id == updated.id) updated else it }
                        },
                        onBack = { screen = Screen.Deck }
                    )
                }
            }
        }
    }
}
