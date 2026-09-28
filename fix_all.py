import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

# Extract isAdmin block
is_admin_pattern = r'  val isAdmin = authStatus\.map \{[\s\S]*?\}\.stateIn\(viewModelScope, SharingStarted\.WhileSubscribed\(5000\), false\)'
is_admin_match = re.search(is_admin_pattern, content)
if is_admin_match:
    is_admin_block = is_admin_match.group(0)
    # Remove from current location
    content = content.replace(is_admin_block + "\n", "")
    
    # Add after authStatus
    auth_status_pattern = r'  val authStatus: StateFlow<AuthStatus> = repository\.authStatus\.stateIn\([\s\S]*?initialValue = AuthStatus\.Initializing\n  \)'
    auth_status_match = re.search(auth_status_pattern, content)
    if auth_status_match:
        auth_status_block = auth_status_match.group(0)
        content = content.replace(auth_status_block, auth_status_block + "\n\n" + is_admin_block)

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content2 = f.read()

content2 = content2.replace("val categories = listOf(\"All\", \"BGMI\", \"Free Fire\", \"COD\", \"Valorant\", \"Joined\", \"High Stakes\")\n\n  matchToEdit?.let", "val categories = listOf(\"All\", \"BGMI\", \"Free Fire\", \"COD\", \"Valorant\", \"Joined\", \"High Stakes\")\n\n  var matchToEdit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<MatchItem?>(null) }\n\n  matchToEdit?.let")

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content2)
