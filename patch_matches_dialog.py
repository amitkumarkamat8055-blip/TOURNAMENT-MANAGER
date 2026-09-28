import re

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()

target = """  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val categories = listOf("All", "BGMI", "Free Fire", "COD", "Valorant", "Joined", "High Stakes")"""

new_state = """  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val categories = listOf("All", "BGMI", "Free Fire", "COD", "Valorant", "Joined", "High Stakes")

  var matchToEdit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<MatchItem?>(null) }
"""

content = content.replace(target, new_state)

target_edit = "onEditClick = { /* TODO: Open Edit Dialog */ }"
new_edit = "onEditClick = { matchToEdit = match }"

content = content.replace(target_edit, new_edit)

dialog = """
  matchToEdit?.let { match ->
    EditMatchDialog(
      match = match,
      onDismiss = { matchToEdit = null },
      onSave = { updatedMatch ->
        viewModel.updateMatch(updatedMatch)
        matchToEdit = null
      }
    )
  }
"""

content = content.replace("  Column(", dialog + "\n  Column(")

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content)
