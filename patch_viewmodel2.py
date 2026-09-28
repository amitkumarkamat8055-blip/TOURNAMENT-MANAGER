import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

new_state = """
  val isAdmin = authStatus.map {
    if (it is AuthStatus.LoggedIn) {
        val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        currentFirebaseUser?.uid == AdminConfig.ADMIN_UID
    } else false
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
"""

content = content.replace("  val authStatus: StateFlow<AuthStatus>", new_state + "\n  val authStatus: StateFlow<AuthStatus>")

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)
