import re

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "r") as f:
    content = f.read()

# Fix imports
if "import com.example.data.model.CustomProfile" not in content:
    content = content.replace("import com.example.data.model.CustomTournament", "import com.example.data.model.CustomTournament\nimport com.example.data.model.CustomProfile")

# Fix variables
target_vars = """  val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()
  val searchQuery by viewModel.customSearchQuery.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var showCreateDialog by remember { mutableStateOf(false) }
  var tournamentToDelete by remember { mutableStateOf<CustomTournament?>(null) }
  var tournamentToJoin by remember { mutableStateOf<CustomTournament?>(null) }"""

replacement_vars = """  val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()
  val firestoreProfiles by viewModel.firestoreCustomProfiles.collectAsStateWithLifecycle()
  val searchQuery by viewModel.customSearchQuery.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  var showCreateDialog by remember { mutableStateOf(false) }
  var profileToDelete by remember { mutableStateOf<CustomProfile?>(null) }
  var profileToJoin by remember { mutableStateOf<CustomProfile?>(null) }
  
  androidx.compose.runtime.LaunchedEffect(Unit) {
    viewModel.fetchCustomProfiles()
  }"""

if target_vars in content:
    content = content.replace(target_vars, replacement_vars)
else:
    # Try another way
    content = re.sub(r"val customTournaments by viewModel\.filteredCustomTournaments\.collectAsStateWithLifecycle\(\)\s+val searchQuery", "val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()\n  val firestoreProfiles by viewModel.firestoreCustomProfiles.collectAsStateWithLifecycle()\n  val searchQuery", content)
    content = re.sub(r"var tournamentToDelete by remember \{ mutableStateOf<CustomTournament\?>\(null\) \}", "var profileToDelete by remember { mutableStateOf<CustomProfile?>(null) }", content)
    content = re.sub(r"var tournamentToJoin by remember \{ mutableStateOf<CustomTournament\?>\(null\) \}", "var profileToJoin by remember { mutableStateOf<CustomProfile?>(null) }\n  androidx.compose.runtime.LaunchedEffect(Unit) {\n    viewModel.fetchCustomProfiles()\n  }", content)

with open("app/src/main/java/com/example/ui/custom/CustomScreen.kt", "w") as f:
    f.write(content)
