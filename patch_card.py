import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

target = """      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
         // Profile info
         Row(verticalAlignment = Alignment.Top) {
           // Avatar with + icon
            Box {
             coil.compose.AsyncImage(
               model = profile.imageUrl,
               contentDescription = "Avatar",
               contentScale = androidx.compose.ui.layout.ContentScale.Crop,
               modifier = Modifier
                 .size(60.dp)
                 .clip(CircleShape)
                 .background(MaterialTheme.colorScheme.surfaceVariant)
             )
             // + Icon
             Box(
               modifier = Modifier
                 .size(20.dp)
                 .clip(CircleShape)
                 .background(MaterialTheme.colorScheme.primary)
                 .align(Alignment.BottomEnd),
               contentAlignment = Alignment.Center
             ) {
               Icon(
                 imageVector = Icons.Default.Add,
                 contentDescription = "Upload",
                 tint = Color.White,
                 modifier = Modifier.size(12.dp)
               )
             }
           }
           
           Spacer(modifier = Modifier.width(16.dp))
           
           // Details
           Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
             // Left Column
             Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
               Text("Name: ${profile.name}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
               Text("UID: ${profile.uid}", style = MaterialTheme.typography.bodyMedium)
               Text("LV: ${profile.level}", style = MaterialTheme.typography.bodyMedium)
               Text("Type: ${profile.type}", style = MaterialTheme.typography.bodyMedium)
             }
             
             // Right Column
             Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
               Text("Gun: ${profile.gun}", style = MaterialTheme.typography.bodyMedium)
               Text("Payout: ${profile.payout}", style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Bold))
               Text("Mode: ${profile.mode}", style = MaterialTheme.typography.bodyMedium)
             }
           }
         }
         
         if (onDelete != null) {
           IconButton(
             onClick = onDelete,
             modifier = Modifier.size(24.dp).padding(end = 4.dp, top = 4.dp)
           ) {
             Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
           }
         }
      }"""

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

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
