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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

private val BgDark      = Color(0xFF0D1117)
private val PanelDark   = Color(0xFF161B22)
private val BorderDim   = Color(0xFF30363D)
private val BorderRed   = Color(0xFFE74C3C)
private val BorderGreen = Color(0xFF2ECC71)

@Composable
fun MainScreen(
    buttons: List<MacroButton>,
    activeIds: Set<Int>,
    onCallResult: (id: Int, success: Boolean) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
            // 3×5 grid
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
                            val btn = buttons[row * 5 + col]
                            MacroButtonCard(
                                button = btn,
                                active = btn.id in activeIds,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = {
                                    if (btn.apiUrl.isBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${btn.label} — no action set")
                                        }
                                        return@MacroButtonCard
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        val success = try {
                                            val conn = URL(btn.apiUrl).openConnection() as HttpURLConnection
                                            conn.requestMethod = "POST"
                                            conn.connectTimeout = 3000
                                            conn.readTimeout = 3000
                                            val code = conn.responseCode
                                            conn.disconnect()
                                            code in 200..299
                                        } catch (e: Exception) {
                                            false
                                        }
                                        onCallResult(btn.id, success)
                                        snackbarHostState.showSnackbar(
                                            if (success) "✓ ${btn.label}" else "No connection to server"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Side panel
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(PanelDark)
                    .border(width = 1.dp, color = BorderDim),
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
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (active) BorderGreen else BorderRed
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(button.colorHex))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
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
        MainScreen(
            buttons = defaultButtons,
            activeIds = emptySet(),
            onCallResult = { _, _ -> },
            onSettingsClick = {}
        )
    }
}
