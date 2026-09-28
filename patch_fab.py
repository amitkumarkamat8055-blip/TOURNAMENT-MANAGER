with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

target = """    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .padding(bottom = 60.dp)
          .testTag("custom_tab_fab_add")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Custom Tournament",
          modifier = Modifier.size(28.dp)
        )
      }
    }"""

replacement = """    floatingActionButton = {
      Box(
        modifier = Modifier
          .padding(bottom = 60.dp)
          .size(56.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
          )
          .clickable { showCreateDialog = true }
          .testTag("custom_tab_fab_add"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Custom Profile",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced!")
else:
    print("Target not found")

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
