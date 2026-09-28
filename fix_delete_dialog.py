import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

target = """  // Delete Tournament Confirmation
  tournamentToDelete?.let { tournament ->
    AlertDialog(
      onDismissRequest = { tournamentToDelete = null },
      title = { Text("Delete Custom Profile") },
      text = { Text("Are you sure you want to remove \\"${tournament.name}\\"?") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCustomTournament(tournament.id)
            tournamentToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
          modifier = Modifier.testTag("confirm_delete_custom_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { tournamentToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }"""

replacement = """  // Delete Tournament Confirmation
  profileToDelete?.let { profile ->
    AlertDialog(
      onDismissRequest = { profileToDelete = null },
      title = { Text("Delete Custom Profile") },
      text = { Text("Are you sure you want to remove \\"${profile.name}\\"?") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCustomProfile(profile.id)
            profileToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
          modifier = Modifier.testTag("confirm_delete_custom_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { profileToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }"""

if target in content:
    content = content.replace(target, replacement)
else:
    # Just simple replace
    content = content.replace("tournamentToDelete", "profileToDelete")
    content = content.replace("tournament.name", "profile.name")
    content = content.replace("tournament.id", "profile.id")
    content = content.replace("viewModel.deleteCustomTournament", "viewModel.deleteCustomProfile")
    content = content.replace("tournament ->", "profile ->")

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
