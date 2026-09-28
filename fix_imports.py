import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.foundation.verticalScroll" not in content:
    content = content.replace("import androidx.compose.foundation.clickable", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState")

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
