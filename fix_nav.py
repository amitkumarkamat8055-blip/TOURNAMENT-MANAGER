import re

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.admin.AdminDashboardScreen", "")
content = "import com.example.ui.admin.AdminDashboardScreen\n" + content

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "r") as f:
    content2 = f.read()
    
content2 = content2.replace("import androidx.compose.material.icons.filled.AdminPanelSettings\nimport androidx.compose.material.icons.outlined.AdminPanelSettings\n", "")
content2 = "import androidx.compose.material.icons.filled.AdminPanelSettings\nimport androidx.compose.material.icons.outlined.AdminPanelSettings\n" + content2

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "w") as f:
    f.write(content2)

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content3 = f.read()
    
content3 = content3.replace("val authStatus: StateFlow<AuthStatus>", "val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()")
with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content3)
