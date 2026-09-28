import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

imports = """
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
"""

new_state = """
  private val _adminUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val adminUsersList: StateFlow<List<Map<String, Any>>> = _adminUsersList.asStateFlow()

  fun fetchAdminUsers() {
    viewModelScope.launch {
      try {
        val snapshot = FirebaseFirestore.getInstance().collection("users").get().await()
        val users = snapshot.documents.map { it.data ?: emptyMap<String, Any>() }
        _adminUsersList.value = users
      } catch (e: Exception) {
        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to fetch users: ${e.message}"))
      }
    }
  }
"""

if "import com.google.firebase.firestore.FirebaseFirestore" not in content:
    content = content.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\n" + imports)

content = content.replace("  // Events", new_state + "\n  // Events")

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)
