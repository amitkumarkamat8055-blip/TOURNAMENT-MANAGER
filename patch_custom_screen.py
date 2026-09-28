import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

# Add LaunchedEffect to call fetchCustomProfiles
if "viewModel.fetchCustomProfiles()" not in content:
    content = content.replace("@Composable\nfun CustomScreen(", "import androidx.compose.runtime.LaunchedEffect\nimport com.example.data.model.CustomProfile\n\n@Composable\nfun CustomScreen(")
    content = content.replace("  var tournamentToJoin by remember { mutableStateOf<CustomTournament?>(null) }", "  var tournamentToJoin by remember { mutableStateOf<CustomTournament?>(null) }\n  var profileToJoin by remember { mutableStateOf<CustomProfile?>(null) }\n\n  LaunchedEffect(Unit) {\n    viewModel.fetchCustomProfiles()\n  }")
    content = content.replace("  val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()", "  val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()\n  val firestoreProfiles by viewModel.firestoreCustomProfiles.collectAsStateWithLifecycle()")

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
