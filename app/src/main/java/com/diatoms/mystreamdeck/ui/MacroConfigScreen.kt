package com.diatoms.mystreamdeck.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diatoms.mystreamdeck.model.MacroButton

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(buttons, key = { it.id }) { button ->
                ListItem(
                    headlineContent = {
                        Text("Button ${button.id} — ${button.label}")
                    },
                    supportingContent = {
                        Text(
                            text = if (button.apiUrl.isNotBlank()) button.apiUrl else "No action configured",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    },
                    modifier = Modifier.clickable { editing = button }
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { button ->
        EditMacroDialog(
            button = button,
            onSave = { updated ->
                onUpdate(updated)
                editing = null
            },
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
    var label by remember { mutableStateOf(button.label) }
    var subLabel by remember { mutableStateOf(button.subLabel ?: "") }
    var apiUrl by remember { mutableStateOf(button.apiUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Button ${button.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subLabel,
                    onValueChange = { subLabel = it },
                    label = { Text("Sub-label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    button.copy(
                        label = label.ifBlank { button.label },
                        subLabel = subLabel.ifBlank { null },
                        apiUrl = apiUrl.trim()
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
