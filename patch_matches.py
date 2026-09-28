import re

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "r") as f:
    content = f.read()

target = """  val matches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val searchQuery by viewModel.matchSearchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()"""

new_val = """  val matches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val searchQuery by viewModel.matchSearchQuery.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()"""

content = content.replace(target, new_val)

target_card = """            MatchCard(
              match = match,
              onCardClick = { onMatchClick(match.id) },
              onJoinClick = { onMatchClick(match.id) }
            )"""

new_card = """            MatchCard(
              match = match,
              isAdmin = isAdmin,
              onCardClick = { onMatchClick(match.id) },
              onJoinClick = { onMatchClick(match.id) },
              onEditClick = { /* TODO: Open Edit Dialog */ }
            )"""
            
content = content.replace(target_card, new_card)

target_def = """fun MatchCard(
  match: MatchItem,
  onCardClick: () -> Unit,
  onJoinClick: () -> Unit,
  modifier: Modifier = Modifier
) {"""

new_def = """import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton

@Composable
fun MatchCard(
  match: MatchItem,
  isAdmin: Boolean = false,
  onCardClick: () -> Unit,
  onJoinClick: () -> Unit,
  onEditClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {"""

content = content.replace("@Composable\n" + target_def, new_def)

# Let's put the Edit button near the top right of the MatchCard
# The card has a Column, then a Row with MatchNumberBadge and StatusBadge
target_row = """        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          MatchNumberBadge(matchNumber = match.matchNumber, format = match.format)
          StatusBadge(status = match.status)
        }"""

new_row = """        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          MatchNumberBadge(matchNumber = match.matchNumber, format = match.format)
          Row(verticalAlignment = Alignment.CenterVertically) {
              StatusBadge(status = match.status)
              if (isAdmin) {
                  IconButton(onClick = onEditClick) {
                      Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Match", tint = MaterialTheme.colorScheme.primary)
                  }
              }
          }
        }"""

content = content.replace(target_row, new_row)

with open("app/src/main/java/com/example/ui/matches/MatchesScreen.kt", "w") as f:
    f.write(content)
