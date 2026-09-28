import re

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()

# Replace all occurrences of the injected dialog + Column( back to just Column( EXCEPT the first one.
dialog_str = """
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

  Column("""

parts = content.split(dialog_str)
if len(parts) > 2:
    new_content = parts[0] + dialog_str + "  Column(".join(parts[1:])
    with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
        f.write(new_content)
