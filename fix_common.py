import re

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material.icons.filled.AdminPanelSettingsimport androidx.compose.material.icons.outlined.AdminPanelSettingsimport", "import androidx.compose.material.icons.filled.AdminPanelSettings\nimport androidx.compose.material.icons.outlined.AdminPanelSettings\nimport")

with open("app/src/main/java/com/example/ui/components/CommonComponents.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "r") as f:
    content = f.read()
    
content = content.replace("package com.example.ui.navigation\n\nimport com.example.ui.admin.AdminDashboardScreenimport", "package com.example.ui.navigation\n\nimport com.example.ui.admin.AdminDashboardScreen\nimport")

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "w") as f:
    f.write(content)
