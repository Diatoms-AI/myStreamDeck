package com.diatoms.mystreamdeck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.diatoms.mystreamdeck.model.MacroButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val SERVER = "http://localhost:8765"

private enum class RecordState { IDLE, RECORDING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroConfigScreen(
    buttons: List<MacroButton>,
    onUpdate: (MacroButton) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var editing by remember { mutableStateOf<MacroButton?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Macros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(buttons, key = { it.id }) { button ->
                ListItem(
                    headlineContent = { Text("Button ${button.id} — ${button.label}") },
                    supportingContent = {
                        Text(
                            text = if (button.apiUrl.isNotBlank()) button.apiUrl
                                   else "No action configured",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingContent = { Icon(Icons.Default.Edit, "Edit") },
                    modifier = Modifier.clickable { editing = button }
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { button ->
        EditMacroDialog(
            button = button,
            onSave = { updated -> onUpdate(updated); editing = null },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun EditMacroDialog(
    button: MacroButton,
    onSave: (MacroButton) -> Unit,
    onDismiss: () -> Unit,
) {
    var label       by remember { mutableStateOf(button.label) }
    var subLabel    by remember { mutableStateOf(button.subLabel ?: "") }
    var apiUrl      by remember { mutableStateOf(button.apiUrl) }
    var recordState by remember { mutableStateOf(RecordState.IDLE) }
    var eventCount  by remember { mutableStateOf(0) }
    var statusMsg   by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Poll /record/status every second while recording
    LaunchedEffect(recordState) {
        if (recordState != RecordState.RECORDING) return@LaunchedEffect
        while (recordState == RecordState.RECORDING) {
            delay(1000)
            withContext(Dispatchers.IO) {
                try {
                    val conn = URL("$SERVER/record/status").openConnection() as HttpURLConnection
                    conn.connectTimeout = 1500; conn.readTimeout = 1500
                    val body = conn.inputStream.bufferedReader().readText()
                    val count = Regex(""""eventCount"\s*:\s*(\d+)""")
                        .find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    withContext(Dispatchers.Main) { eventCount = count }
                } catch (_: Exception) {}
            }
        }
    }

    fun startRecording() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = URL("$SERVER/record/start?id=${button.id}")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000; conn.readTimeout = 3000
                conn.responseCode
                withContext(Dispatchers.Main) {
                    eventCount = 0
                    recordState = RecordState.RECORDING
                    statusMsg = ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMsg = "Cannot reach server" }
            }
        }
    }

    fun stopRecording() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = URL("$SERVER/record/stop").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000; conn.readTimeout = 3000
                conn.responseCode
                withContext(Dispatchers.Main) {
                    // Auto-save: update the button's URL and close the dialog
                    onSave(button.copy(
                        label    = label.ifBlank { button.label },
                        subLabel = subLabel.ifBlank { null },
                        apiUrl   = "$SERVER/button/${button.id}"
                    ))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    recordState = RecordState.IDLE
                    statusMsg = "Stop failed: ${e.message}"
                }
            }
        }
    }

    fun resetFields() {
        label       = "#${button.id}"
        subLabel    = ""
        apiUrl      = ""
        recordState = RecordState.IDLE
        statusMsg   = ""
        eventCount  = 0
    }

    Dialog(onDismissRequest = {
        if (recordState == RecordState.RECORDING) stopRecording()
        else onDismiss()
    }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Button ${button.id}", style = MaterialTheme.typography.headlineSmall)

                if (recordState == RecordState.RECORDING) {
                    // ── Recording mode ────────────────────────────────
                    RecordingIndicator(eventCount = eventCount)

                    Text(
                        "Perform your actions on the PC.\nThis screen is not recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = ::stopRecording,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Done")
                    }

                } else {
                    // ── Edit mode ─────────────────────────────────────
                    OutlinedTextField(
                        value = label, onValueChange = { label = it },
                        label = { Text("Label") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subLabel, onValueChange = { subLabel = it },
                        label = { Text("Sub-label (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiUrl, onValueChange = { apiUrl = it },
                        label = { Text("API URL") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (statusMsg.isNotBlank()) {
                        Text(
                            statusMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Cancel · Reset · ——— · Record · Save
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }

                        TextButton(onClick = ::resetFields) { Text("Reset") }

                        Spacer(Modifier.weight(1f))

                        OutlinedButton(
                            onClick = ::startRecording,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.FiberManualRecord, null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFE74C3C)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Record")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = {
                            onSave(button.copy(
                                label    = label.ifBlank { button.label },
                                subLabel = subLabel.ifBlank { null },
                                apiUrl   = apiUrl.trim()
                            ))
                        }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingIndicator(eventCount: Int) {
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(600); blink = !blink } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFFE74C3C), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (blink) Color(0xFFE74C3C) else Color.Transparent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Recording on PC…  $eventCount event${if (eventCount != 1) "s" else ""} captured",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}
