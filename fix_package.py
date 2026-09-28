import re

for filename in ["app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "app/src/main/java/com/example/ui/components/CommonComponents.kt"]:
    with open(filename, "r") as f:
        content = f.read()
        
    pkg = re.search(r'package\s+[\w\.]+', content)
    if pkg:
        pkg_str = pkg.group(0)
        content = content.replace(pkg_str, "")
        content = pkg_str + "\n\n" + content
        
    with open(filename, "w") as f:
        f.write(content)
        
with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

# Fix authStatus initialization
content = content.replace("val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()\n  val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()", "val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()")
content = content.replace("val authStatus: StateFlow<AuthStatus>", "val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()")
content = content.replace("val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow() = _authStatus.asStateFlow()", "val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()")

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)

