import re

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.runtime.setValue" not in content:
    content = content.replace("import androidx.compose.runtime.getValue", "import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue")

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content)
