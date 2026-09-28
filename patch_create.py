import os

new_code = """
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
  onDismiss: () -> Unit,
  onCreate: (
    name: String,
    uid: String,
    level: Int,
    payout: Int,
    regFee: Int,
    type: String,
    mode: String,
    gun: String,
    imageUriStr: String
  ) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var uid by remember { mutableStateOf(defaultUid) }
  var levelStr by remember { mutableStateOf("") }
  var payoutStr by remember { mutableStateOf("") }
  var regFeeStr by remember { mutableStateOf("") }
  
  var type by remember { mutableStateOf("1VS1") }
  var mode by remember { mutableStateOf("Body") }
  var gun by remember { mutableStateOf("UMP") }
  
  var imageUri by remember { mutableStateOf<Uri?>(null) }
  
  val typeOptions = listOf("1VS1", "2VS2", "4VS4")
  val modeOptions = listOf("Body", "Headshot")
  val gunOptions = listOf("UMP", "All", "Headshot Gun")
  
  var typeExpanded by remember { mutableStateOf(false) }
  var modeExpanded by remember { mutableStateOf(false) }
  var gunExpanded by remember { mutableStateOf(false) }
  
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      imageUri = uri
  }
  
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Create Custom Profile",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00FF7F))
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
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
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = levelStr,
              onValueChange = { levelStr = it },
              label = { Text("LV (Min 51)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
              isError = levelStr.isNotEmpty() && (levelStr.toIntOrNull() ?: 0) < 51
            )
            OutlinedTextField(
              value = payoutStr,
              onValueChange = { payoutStr = it },
              label = { Text("Payout (₹)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
        }
        
        OutlinedTextField(
          value = regFeeStr,
          onValueChange = { regFeeStr = it },
          label = { Text("REG. FEE (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        
        Text("Match Settings", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = Color(0xFF00FF7F))
        
        // Dropdowns
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("TYPE") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                typeOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                }
            }
        }
        
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
    },
    confirmButton = {
      Button(
        onClick = {
            val lv = levelStr.toIntOrNull() ?: 0
            if (name.isNotBlank() && uid.isNotBlank() && lv >= 51 && payoutStr.toIntOrNull() != null && regFeeStr.toIntOrNull() != null) {
                onCreate(
                    name, uid, lv, payoutStr.toIntOrNull() ?: 0, regFeeStr.toIntOrNull() ?: 0,
                    type, mode, gun, imageUri?.toString() ?: ""
                )
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F), contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Create & Publish", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color.Gray)
      }
    }
  )
}
"""

with open("app/src/main/java/com/example/ui/custom/CreateProfileTournamentDialog.kt", "w") as f:
    f.write(new_code)
