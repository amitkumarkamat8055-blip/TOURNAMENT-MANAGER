import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

target = """      // Search Bar
      AppSearchBar(
        query = searchQuery,
        onQueryChange = { viewModel.setCustomSearchQuery(it) },
        placeholder = "Search custom profile, UID, region..."
      )

      Spacer(modifier = Modifier.height(10.dp))"""

replacement = """      // Search Bar
      AppSearchBar(
        query = searchQuery,
        onQueryChange = { viewModel.setCustomSearchQuery(it) },
        placeholder = "Search custom profile, UID, region..."
      )

      // Game Filters
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        androidx.compose.material3.FilterChip(
          selected = false,
          onClick = { },
          label = { Text("freefire") },
          colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
             containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(16.dp)
        )
        androidx.compose.material3.FilterChip(
          selected = false,
          onClick = { },
          label = { Text("pubg") },
          colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
             containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(16.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))"""

if target in content:
    content = content.replace(target, replacement)
else:
    print("Not found")

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
