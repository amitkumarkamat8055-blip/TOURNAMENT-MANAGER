import re

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()

content = content.replace('  val categories = listOf("All", "BGMI", "Free Fire", "COD", "Valorant", "Joined", "High Stakes")\n  matchToEdit?.let', '  val categories = listOf("All", "BGMI", "Free Fire", "COD", "Valorant", "Joined", "High Stakes")\n  var matchToEdit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<MatchItem?>(null) }\n  matchToEdit?.let')

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content)
