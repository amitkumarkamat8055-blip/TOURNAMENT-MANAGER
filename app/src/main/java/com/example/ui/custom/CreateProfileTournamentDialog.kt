
package com.example.ui.custom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileTournamentDialog(
  defaultUid: String,
  defaultRegion: String,
  profileToEdit: com.example.data.model.CustomProfile? = null,
  onDismiss: () -> Unit,
  onCreate: (
    name: String,
    uid: String,
    level: Int,
    payout: Int,
    prizePool: Int,
    perKill: Int,
    totalPlayers: Int,
    category: String,
    game: String,
    day: String,
    time: String,
    type: String,
    mode: String,
    gun: String,
    imageUriStr: String
  ) -> Unit
) {
  val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

  var name by remember { mutableStateOf(profileToEdit?.name ?: "") }
  var uid by remember { mutableStateOf(profileToEdit?.uid ?: defaultUid) }
  var levelStr by remember { mutableStateOf(profileToEdit?.level?.toString() ?: "") }
  var payoutStr by remember { mutableStateOf(profileToEdit?.payout?.toString() ?: "") }
  var prizePoolStr by remember { mutableStateOf(profileToEdit?.prizePool?.toString() ?: "") }
  var perKillYesNo by remember { mutableStateOf(if (profileToEdit?.category == "BR" && (profileToEdit.perKill.isNotBlank() && profileToEdit.perKill != "0")) "Yes" else "No") }
  var perKillStr by remember { mutableStateOf(if (profileToEdit?.perKill != "0") profileToEdit?.perKill ?: "" else "") }
  var totalPlayersStr by remember { mutableStateOf(if (profileToEdit?.totalPlayers != "0") profileToEdit?.totalPlayers ?: "" else "") }
  
  var category by remember { mutableStateOf(if (profileToEdit?.category?.isNotBlank() == true) profileToEdit.category else "Custom") }
  
  val dayOptions = remember {
      val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
      (0 until 12).map { offset ->
          val cal = java.util.Calendar.getInstance()
          cal.add(java.util.Calendar.DAY_OF_YEAR, offset)
          val dStr = dateFormat.format(cal.time)
          when (offset) {
              0 -> "Today, $dStr"
              1 -> "Tomorrow, $dStr"
              else -> dStr
          }
      }
  }
  
  var game by remember { mutableStateOf(if (profileToEdit?.game?.isNotBlank() == true) profileToEdit.game else "Lone Wolf") }
  var day by remember { 
      mutableStateOf(
          if (profileToEdit?.day?.isNotBlank() == true) {
              val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
              val cal = java.util.Calendar.getInstance()
              val todayStr = dateFormat.format(cal.time)
              val calTom = java.util.Calendar.getInstance()
              calTom.add(java.util.Calendar.DAY_OF_YEAR, 1)
              val tomStr = dateFormat.format(calTom.time)
              val trimmed = profileToEdit.day.trim()
              when {
                  trimmed.equals("Today", ignoreCase = true) || trimmed.equals(todayStr, ignoreCase = true) -> "Today, $todayStr"
                  trimmed.equals("Tomorrow", ignoreCase = true) || trimmed.equals(tomStr, ignoreCase = true) -> "Tomorrow, $tomStr"
                  trimmed.startsWith("Today, ", ignoreCase = true) || trimmed.startsWith("Tomorrow, ", ignoreCase = true) -> trimmed
                  else -> trimmed
              }
          } else dayOptions.first()
      ) 
  }
  var time by remember { mutableStateOf(if (profileToEdit?.time?.isNotBlank() == true) profileToEdit.time else "10:00 AM") }
  var type by remember { mutableStateOf(if (profileToEdit?.type?.isNotBlank() == true) profileToEdit.type else "1VS1") }
  var mode by remember { mutableStateOf(
      if (profileToEdit?.category == "BR") {
          if (profileToEdit.mode.startsWith("Specific Mode")) "Specific Mode" else "Esports Mode"
      } else {
          if (profileToEdit?.mode?.isNotBlank() == true) profileToEdit.mode else "Body"
      }
  ) }
  var hpStr by remember { mutableStateOf(
      if (profileToEdit?.category == "BR" && profileToEdit.mode.contains("HP")) {
          profileToEdit.mode.substringAfterLast("| ").substringBefore(" HP")
      } else ""
  ) }
  var specificGunYesNo by remember { mutableStateOf(
      if (profileToEdit?.category == "BR" && profileToEdit.mode.startsWith("Specific Mode")) {
          if (profileToEdit.gun == "Body" || profileToEdit.gun == "Headshot" || profileToEdit.gun == "All") "No" else "Yes"
      } else "No"
  ) }
  var gunName by remember { mutableStateOf(
      if (profileToEdit?.category == "BR" && profileToEdit.gun != "Body" && profileToEdit.gun != "Headshot" && profileToEdit.gun != "All") {
          if (profileToEdit.gun.contains("|")) profileToEdit.gun.substringBefore(" |") else profileToEdit.gun
      } else ""
  ) }
  var bodyHeadshot by remember { mutableStateOf(
      if (profileToEdit?.category == "BR") {
          if (profileToEdit.gun.contains("| ")) profileToEdit.gun.substringAfter("| ")
          else if (profileToEdit.gun == "Body" || profileToEdit.gun == "Headshot") profileToEdit.gun
          else "Body"
      } else "Body"
  ) }
  var gun by remember { mutableStateOf(if (profileToEdit?.gun?.isNotBlank() == true) profileToEdit.gun else "UMP") }
  
  var imageUri by remember { mutableStateOf<Uri?>(if (profileToEdit?.imageUrl?.isNotBlank() == true) Uri.parse(profileToEdit.imageUrl) else null) }
  
  val gameOptions = listOf("Lone Wolf", "Custom")
  val timeOptions = listOf("10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM", "9:00 PM", "10:00 PM")
  val typeOptions = if (category == "BR") listOf("Solo", "Duo") else listOf("1VS1", "2VS2", "4VS4")
  val modeOptions = if (category == "BR") listOf("Esports Mode", "Specific Mode") else listOf("Body", "Headshot")
  val gunOptions = listOf("UMP", "All", "Headshot Gun")
  val specificGunOptions = listOf("Yes", "No")
  val bodyHeadshotOptions = listOf("Body", "Headshot")
  
  var gameExpanded by remember { mutableStateOf(false) }
  var dayExpanded by remember { mutableStateOf(false) }
  var timeExpanded by remember { mutableStateOf(false) }
  var typeExpanded by remember { mutableStateOf(false) }
  var modeExpanded by remember { mutableStateOf(false) }
  var gunExpanded by remember { mutableStateOf(false) }
  var specificGunExpanded by remember { mutableStateOf(false) }
  var bodyHeadshotExpanded by remember { mutableStateOf(false) }
  
  val context = androidx.compose.ui.platform.LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      imageUri = uri
  }
  
  val safeDismiss = {
    keyboardController?.hide()
    focusManager.clearFocus()
    onDismiss()
  }

  AlertDialog(
    onDismissRequest = safeDismiss,
    properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false),
    title = {
      Text(
        text = "Create Custom Profile",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Profile Picture Upload
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .clickable { launcher.launch("image/*") },
          contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = Color.White)
            }
        }
        
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
          value = uid,
          onValueChange = { uid = it },
          label = { Text("UID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        
        // Category Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    category = "Custom"
                    type = "1VS1"
                    mode = "Body"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (category == "Custom") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (category == "Custom") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Custom")
            }
            Button(
                onClick = { 
                    category = "BR"
                    type = "Solo"
                    mode = "Esports Mode"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (category == "BR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (category == "BR") Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BR Room")
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = payoutStr,
              onValueChange = { payoutStr = it },
              label = { Text(if (category == "BR") "Entry Fee (₹)" else "Payout (₹)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (category == "BR") {
            OutlinedTextField(
              value = prizePoolStr,
              onValueChange = { prizePoolStr = it },
              label = { Text("Prize Pool (₹)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
              value = totalPlayersStr,
              onValueChange = { totalPlayersStr = it },
              label = { Text("Total Players") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Per Kill?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = perKillYesNo == "Yes", onClick = { perKillYesNo = "Yes" })
                    Text("Yes", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = perKillYesNo == "No", onClick = { perKillYesNo = "No"; perKillStr = "" })
                    Text("No", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (perKillYesNo == "Yes") {
                OutlinedTextField(
                    value = perKillStr,
                    onValueChange = { perKillStr = it },
                    label = { Text("Per Kill Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Match Schedule: Date & Time Dropdowns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date / Day Dropdown with Date Selection
            ExposedDropdownMenuBox(
                expanded = dayExpanded,
                onExpandedChange = { dayExpanded = !dayExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = day,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("DATE") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                    dayOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                day = option
                                dayExpanded = false
                            }
                        )
                    }
                }
            }

            // Match Time Dropdown
            ExposedDropdownMenuBox(
                expanded = timeExpanded,
                onExpandedChange = { timeExpanded = !timeExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("TIME") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { timeExpanded = false }) {
                    timeOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { time = option; timeExpanded = false })
                    }
                }
            }
        }
        
        Text("Match Settings", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.primary)
        
        OutlinedTextField(
            value = levelStr,
            onValueChange = { levelStr = it },
            label = { Text("LV (Min 51)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = levelStr.isNotEmpty() && (levelStr.toIntOrNull() ?: 0) < 51
        )
        
        // Game / Mode Dropdown (Top)
        if (category == "Custom") {
            ExposedDropdownMenuBox(expanded = gameExpanded, onExpandedChange = { gameExpanded = !gameExpanded }) {
                OutlinedTextField(
                    value = game,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("GAME") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gameExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = gameExpanded, onDismissRequest = { gameExpanded = false }) {
                    gameOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { game = option; gameExpanded = false })
                    }
                }
            }
        } else {
            ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = !modeExpanded }) {
                OutlinedTextField(
                    value = mode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("MODE") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                    modeOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { mode = option; modeExpanded = false })
                    }
                }
            }
        }

        // Type Dropdown
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (category == "BR") "FORMAT (TYPE)" else "TYPE") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                typeOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                }
            }
        }
        
        if (category == "BR" && mode == "Specific Mode") {
            OutlinedTextField(
                value = hpStr,
                onValueChange = { hpStr = it },
                label = { Text("HP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenuBox(expanded = specificGunExpanded, onExpandedChange = { specificGunExpanded = !specificGunExpanded }) {
                OutlinedTextField(
                    value = specificGunYesNo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Specific Gun") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specificGunExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = specificGunExpanded, onDismissRequest = { specificGunExpanded = false }) {
                    specificGunOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { specificGunYesNo = option; specificGunExpanded = false })
                    }
                }
            }
            
            if (specificGunYesNo == "Yes") {
                OutlinedTextField(
                    value = gunName,
                    onValueChange = { gunName = it },
                    label = { Text("Gun Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            ExposedDropdownMenuBox(expanded = bodyHeadshotExpanded, onExpandedChange = { bodyHeadshotExpanded = !bodyHeadshotExpanded }) {
                OutlinedTextField(
                    value = bodyHeadshot,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Body / Headshot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bodyHeadshotExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = bodyHeadshotExpanded, onDismissRequest = { bodyHeadshotExpanded = false }) {
                    bodyHeadshotOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { bodyHeadshot = option; bodyHeadshotExpanded = false })
                    }
                }
            }
        }
        
        if (category == "Custom") {
            ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = !modeExpanded }) {
                OutlinedTextField(
                    value = mode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("MODE") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                    modeOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { mode = option; modeExpanded = false })
                    }
                }
            }
        }
        
        if (category == "Custom" && game != "Lone Wolf") {
            ExposedDropdownMenuBox(expanded = gunExpanded, onExpandedChange = { gunExpanded = !gunExpanded }) {
                OutlinedTextField(
                    value = gun,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("GUN") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gunExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = gunExpanded, onDismissRequest = { gunExpanded = false }) {
                    gunOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { gun = option; gunExpanded = false })
                    }
                }
            }
        }
      }
    },
    confirmButton = {
      val lv = levelStr.toIntOrNull() ?: 0
      val isFormValid = name.isNotBlank() && uid.isNotBlank() && levelStr.isNotBlank() && payoutStr.isNotBlank() && 
                        (category != "BR" || (prizePoolStr.isNotBlank() && totalPlayersStr.isNotBlank() && (perKillYesNo == "No" || perKillStr.isNotBlank()))) && lv >= 51

      Button(
        onClick = {
            val modeToSave = if (category == "BR" && mode == "Specific Mode") {
                if (hpStr.isNotBlank()) "Specific Mode | $hpStr HP" else "Specific Mode"
            } else {
                mode
            }
            val gunToSave = if (category == "BR") {
                if (mode == "Specific Mode") {
                    if (specificGunYesNo == "Yes") {
                        if (gunName.isNotBlank()) "$gunName | $bodyHeadshot" else bodyHeadshot
                    } else {
                        bodyHeadshot
                    }
                } else {
                    "All"
                }
            } else {
                if (game == "Lone Wolf") "Lone Wolf" else gun
            }
            
            keyboardController?.hide()
            focusManager.clearFocus()
            onCreate(
                name, uid, lv, payoutStr.toIntOrNull() ?: 0, prizePoolStr.toIntOrNull() ?: 0, 
                perKillStr.toIntOrNull() ?: 0, totalPlayersStr.toIntOrNull() ?: 0, category,
                game, day, time, type, modeToSave, gunToSave, imageUri?.toString() ?: ""
            )
        },
        enabled = isFormValid,
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              if (isFormValid) {
                  Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
              } else {
                  Brush.linearGradient(
                    listOf(Color.Gray, Color.DarkGray)
                  )
              }
            )
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(if (profileToEdit != null) "Update & Publish" else "Create & Publish", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onDismiss()
        }
      ) {
        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}
