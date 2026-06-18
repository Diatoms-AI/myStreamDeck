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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diatoms.mystreamdeck.model.MacroButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val SERVER = "http://localhost:8765"

private enum class RecordState { IDLE, RECORDING, DONE }

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
    var label    by remember { mutableStateOf(button.label) }
    var subLabel by remember { mutableStateOf(button.subLabel ?: "") }
    var apiUrl   by remember { mutableStateOf(button.apiUrl) }

    var recordState  by remember { mutableStateOf(RecordState.IDLE) }
    var eventCount   by remember { mutableStateOf(0) }
    var statusMsg    by remember { mutableStateOf("") }
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
                val body = conn.inputStream.bufferedReader().readText()
                val count = Regex(""""eventCount"\s*:\s*(\d+)""")
                    .find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                withContext(Dispatchers.Main) {
                    eventCount = count
                    recordState = RecordState.DONE
                    // Auto-set URL so button press triggers playback
                    apiUrl = "$SERVER/button/${button.id}"
                    statusMsg = "$count events recorded"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    recordState = RecordState.IDLE
                    statusMsg = "Stop failed: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (recordState == RecordState.RECORDING) stopRecording()
            onDismiss()
        },
        title = { Text("Button ${button.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Label fields ──────────────────────────────────────
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

                HorizontalDivider()

                // ── Macro recorder ────────────────────────────────────
                Text("Screen Macro Recorder", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                when (recordState) {
                    RecordState.IDLE -> {
                        Button(
                            onClick = ::startRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FiberManualRecord, null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Record Macro")
                        }
                        if (statusMsg.isNotBlank())
                            Text(statusMsg, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    RecordState.RECORDING -> {
                        RecordingIndicator(eventCount = eventCount)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = ::stopRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop & Save")
                        }
                    }

                    RecordState.DONE -> {
                        Text(
                            "✓ $statusMsg — URL set to server",
                            color = Color(0xFF2ECC71),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = { recordState = RecordState.IDLE; statusMsg = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Record Again") }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(button.copy(
                    label    = label.ifBlank { button.label },
                    subLabel = subLabel.ifBlank { null },
                    apiUrl   = apiUrl.trim()
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (recordState == RecordState.RECORDING) stopRecording()
                onDismiss()
            }) { Text("Cancel") }
        }
    )
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
