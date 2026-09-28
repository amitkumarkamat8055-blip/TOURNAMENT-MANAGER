import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

# Replace any malformed authStatus declaration
content = re.sub(r'val authStatus:\s*StateFlow<AuthStatus>[\s\S]*?=\s*repository\.authStatus\.stateIn', 'val authStatus: StateFlow<AuthStatus> = repository.authStatus.stateIn', content)

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)
