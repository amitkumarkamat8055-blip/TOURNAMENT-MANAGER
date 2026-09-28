import re

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.admin.AdminDashboardScreenimport", "import com.example.ui.admin.AdminDashboardScreen\nimport")

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "w") as f:
    f.write(content)
    
with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()
    
content = content.replace("import androidx.compose.material3.IconButtonimport", "import androidx.compose.material3.IconButton\nimport")

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content)
