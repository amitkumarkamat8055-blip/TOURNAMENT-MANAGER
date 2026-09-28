package com.example.ui.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MatchItem

@Composable
fun EditMatchDialog(
    match: MatchItem,
    onDismiss: () -> Unit,
    onSave: (MatchItem) -> Unit
) {
    var entryFeeStr by remember { mutableStateOf(match.entryFee.toString()) }
    var prizeStr by remember { mutableStateOf(match.prizePool.toString()) }
    var rules by remember { mutableStateOf(match.rules) }
    var maxPlayersStr by remember { mutableStateOf(match.maxPlayers.toString()) }
    var date by remember { mutableStateOf(match.date) }
    var time by remember { mutableStateOf(match.time) }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val safeDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Edit Match ${match.matchNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = entryFeeStr,
                    onValueChange = { entryFeeStr = it },
                    label = { Text("Entry Fee (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prizeStr,
                    onValueChange = { prizeStr = it },
                    label = { Text("Prize Pool (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxPlayersStr,
                    onValueChange = { maxPlayersStr = it },
                    label = { Text("Max Players") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("Date Selection", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today", "Tomorrow").forEach { dayOpt ->
                        FilterChip(
                            selected = date.equals(dayOpt, ignoreCase = true),
                            onClick = { date = dayOpt },
                            label = { Text(dayOpt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g. Today, Tomorrow, 15 Aug)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 08:30 PM)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Rules") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val entryFee = entryFeeStr.toIntOrNull() ?: match.entryFee
                    val prize = prizeStr.toIntOrNull() ?: match.prizePool
                    val maxP = maxPlayersStr.toIntOrNull() ?: match.maxPlayers
                    onSave(
                        match.copy(
                            entryFee = entryFee,
                            prizePool = prize,
                            maxPlayers = maxP,
                            date = date,
                            time = time,
                            rules = rules
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) {
                Text("Cancel")
            }
        }
    )
}
