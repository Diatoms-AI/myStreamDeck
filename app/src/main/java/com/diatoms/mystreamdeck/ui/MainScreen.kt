package com.diatoms.mystreamdeck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diatoms.mystreamdeck.model.MacroButton
import com.diatoms.mystreamdeck.model.defaultButtons
import com.diatoms.mystreamdeck.ui.theme.MyStreamDeckTheme

private val BgDark = Color(0xFF0D1117)
private val PanelDark = Color(0xFF161B22)
private val BorderColor = Color(0xFF30363D)

@Composable
fun MainScreen(
    buttons: List<MacroButton>,
    onSettingsClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgDark
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgDark)
        ) {
            // 3×5 grid — weight-based so all 15 buttons always fill the screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..4) {
                            MacroButtonCard(
                                button = buttons[row * 5 + col],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = {
                                    val b = buttons[row * 5 + col]
                                    snackbarMessage = if (b.apiUrl.isNotBlank())
                                        "Calling: ${b.apiUrl}"
                                    else
                                        "${b.label} — no action set"
                                }
                            )
                        }
                    }
                }
            }

            // Side panel — settings gear only
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(PanelDark)
                    .border(width = 1.dp, color = BorderColor),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure macros",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroButtonCard(
    button: MacroButton,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(button.colorHex))
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = button.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (button.subLabel != null) {
                Text(
                    text = button.subLabel,
                    color = Color(0xFF58A6FF),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun MainScreenPreview() {
    MyStreamDeckTheme {
        MainScreen(buttons = defaultButtons, onSettingsClick = {})
    }
}
