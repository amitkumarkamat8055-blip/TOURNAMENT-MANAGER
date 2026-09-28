import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

pattern = r"""      Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.Top\s*\)\s*\{\s*// Profile info.*?if \(onDelete != null\) \{\s*IconButton\(\s*onClick = onDelete,\s*modifier = Modifier\.size\(24\.dp\)\.padding\(end = 4\.dp, top = 4\.dp\)\s*\)\s*\{\s*Icon\(imageVector = Icons\.Default\.Delete, contentDescription = "Delete", tint = AlertRed\)\s*\}\s*\}\s*\}"""

replacement = """      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
         // Profile info
         Row(
             verticalAlignment = Alignment.CenterVertically,
             modifier = Modifier.weight(1f)
         ) {
           // Avatar (no plus icon needed for display)
           coil.compose.AsyncImage(
             model = profile.imageUrl,
             contentDescription = "Avatar",
             contentScale = androidx.compose.ui.layout.ContentScale.Crop,
             modifier = Modifier
               .size(54.dp)
               .clip(CircleShape)
               .background(MaterialTheme.colorScheme.surfaceVariant)
           )
           
           Spacer(modifier = Modifier.width(16.dp))
           
           // Middle Column
           Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
             Text("Name - ${profile.name}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
             Text("UID - ${profile.uid}", style = MaterialTheme.typography.bodyMedium)
             Text("L.v = ${profile.level}", style = MaterialTheme.typography.bodyMedium)
             Text("Type ${profile.type}", style = MaterialTheme.typography.bodyMedium)
           }
         }
         
         // Right Column
         Column(
             verticalArrangement = Arrangement.spacedBy(4.dp),
             horizontalAlignment = Alignment.Start
         ) {
             if (onDelete != null) {
               IconButton(
                 onClick = onDelete,
                 modifier = Modifier.size(24.dp).align(Alignment.End).padding(bottom = 8.dp)
               ) {
                 Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
               }
             }
             Text("Mode: ${profile.mode}", style = MaterialTheme.typography.bodyMedium)
             Text("Payout= ₹${profile.payout}", style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Bold))
             Text("Gun= ${profile.gun}", style = MaterialTheme.typography.bodyMedium)
         }
      }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
if new_content == content:
    print("Not found with regex either")
else:
    print("Replaced!")
    with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
        f.write(new_content)
