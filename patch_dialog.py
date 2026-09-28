import re

with open("app/src/main/java/com/example/ui/custom/CreateProfileTournamentDialog.kt", "r") as f:
    content = f.read()

# Fix imports
if "import androidx.compose.ui.graphics.Brush" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush")

# Fix title color
content = content.replace("color = Color(0xFF00FF7F)", "color = MaterialTheme.colorScheme.primary")

# Fix Create & Publish Button
target_btn = """      Button(
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
      }"""

replacement_btn = """      Button(
        onClick = {
            val lv = levelStr.toIntOrNull() ?: 0
            if (name.isNotBlank() && uid.isNotBlank() && lv >= 51 && payoutStr.toIntOrNull() != null && regFeeStr.toIntOrNull() != null) {
                onCreate(
                    name, uid, lv, payoutStr.toIntOrNull() ?: 0, regFeeStr.toIntOrNull() ?: 0,
                    type, mode, gun, imageUri?.toString() ?: ""
                )
            }
        },
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
              )
            )
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Create & Publish", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }"""

# A fallback in case exact replace fails
content = re.sub(
    r"colors = ButtonDefaults\.buttonColors\(containerColor = Color\(0xFF00FF7F\), contentColor = Color\.Black\),\n\s*shape = RoundedCornerShape\(12\.dp\),\n\s*modifier = Modifier\.fillMaxWidth\(\)\n\s*\)\s*\{\n\s*Text\(\"Create & Publish\", fontWeight = FontWeight\.Bold\)\n\s*\}",
    """contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
              )
            )
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Create & Publish", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }""",
    content
)

# Replace 'color = Color.Gray' with onSurfaceVariant
content = content.replace("color = Color.Gray", "color = MaterialTheme.colorScheme.onSurfaceVariant")


with open("app/src/main/java/com/example/ui/custom/CreateProfileTournamentDialog.kt", "w") as f:
    f.write(content)
