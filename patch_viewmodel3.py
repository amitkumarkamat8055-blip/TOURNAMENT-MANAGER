import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

new_method = """
  fun updateMatch(match: MatchItem) {
    viewModelScope.launch {
      repository.updateMatch(match)
      _uiEvents.emit(UiEvent.ShowSnackbar("Match updated successfully!"))
    }
  }
"""

content = content.replace("  // Update Profile", new_method + "\n  // Update Profile")

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)
