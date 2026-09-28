package com.example.ui.custom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CustomProfile

data class PlayerResult(var name: String = "", var uid: String = "", var prize: String = "")
data class KillResult(var name: String = "", var uid: String = "", var kills: String = "", var amount: String = "")

@Composable
fun BRMatchResultDialog(
    profile: CustomProfile,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val perKillStr = profile.perKill.trim()
    val hasPerKill = perKillStr.isNotEmpty() && perKillStr != "0" && perKillStr.toIntOrNull() != 0

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUri = uri }

    val firstPlace = remember { mutableStateOf(PlayerResult()) }
    val secondPlace = remember { mutableStateOf(PlayerResult()) }
    val thirdPlace = remember { mutableStateOf(PlayerResult()) }
    
    val killResults = remember { mutableStateListOf<KillResult>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Match Result", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo Upload
                Column {
                    Text("Match Result Photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Match Result Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoUri == null) "Upload Photo" else "Change Photo")
                    }
                }

                HorizontalDivider()

                // Winners Section
                Text("Winners", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                
                PlayerResultInput("1st Place", firstPlace.value) { firstPlace.value = it }
                PlayerResultInput("2nd Place (Optional)", secondPlace.value) { secondPlace.value = it }
                PlayerResultInput("3rd Place (Optional)", thirdPlace.value) { thirdPlace.value = it }

                if (hasPerKill) {
                    HorizontalDivider()
                    Text("Per Kill Payouts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    
                    killResults.forEachIndexed { index, killResult ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Player ${index + 1}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { killResults.removeAt(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                OutlinedTextField(
                                    value = killResult.name,
                                    onValueChange = { killResults[index] = killResult.copy(name = it) },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = killResult.uid,
                                    onValueChange = { killResults[index] = killResult.copy(uid = it) },
                                    label = { Text("UID") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = killResult.kills,
                                        onValueChange = { killResults[index] = killResult.copy(kills = it) },
                                        label = { Text("Kills") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = killResult.amount,
                                        onValueChange = { killResults[index] = killResult.copy(amount = it) },
                                        label = { Text("Amount (₹)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { killResults.add(KillResult()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Kill Payout")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = firstPlace.value.name.isNotBlank() && firstPlace.value.uid.isNotBlank() && firstPlace.value.prize.isNotBlank()
            ) {
                Text("Submit Result")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PlayerResultInput(
    label: String,
    value: PlayerResult,
    onValueChange: (PlayerResult) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = value.name,
                onValueChange = { onValueChange(value.copy(name = it)) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value.uid,
                    onValueChange = { onValueChange(value.copy(uid = it)) },
                    label = { Text("UID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = value.prize,
                    onValueChange = { onValueChange(value.copy(prize = it)) },
                    label = { Text("Prize (₹)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
