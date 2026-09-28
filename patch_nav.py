import re

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "r") as f:
    content = f.read()

target_nav = """      MainContainerScreen(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onMatchClick = { matchId ->
          navController.navigate(Screen.MatchDetails.createRoute(matchId))
        },
        onNavigateToAuth = {
          viewModel.logout()
        }
      )"""

new_nav = """      val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
      MainContainerScreen(
        viewModel = viewModel,
        isAdmin = isAdmin,
        snackbarHostState = snackbarHostState,
        onMatchClick = { matchId ->
          navController.navigate(Screen.MatchDetails.createRoute(matchId))
        },
        onNavigateToAuth = {
          viewModel.logout()
        }
      )"""

content = content.replace(target_nav, new_nav)

target_screen = """@Composable
fun MainContainerScreen(
  viewModel: TournamentViewModel,
  snackbarHostState: SnackbarHostState,"""

new_screen = """import com.example.ui.admin.AdminDashboardScreen

@Composable
fun MainContainerScreen(
  viewModel: TournamentViewModel,
  isAdmin: Boolean,
  snackbarHostState: SnackbarHostState,"""

content = content.replace(target_screen, new_screen)

target_bottom = """    bottomBar = {
      BottomNavBar(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.selectTab(it) }
      )
    },"""

new_bottom = """    bottomBar = {
      BottomNavBar(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.selectTab(it) },
        isAdmin = isAdmin
      )
    },"""

content = content.replace(target_bottom, new_bottom)

target_when = """      when (tab) {
        0 -> MatchesScreen(
          viewModel = viewModel,
          onMatchClick = onMatchClick,
          onProfileClick = { viewModel.selectTab(if (isAdmin) 3 else 2) }
        )
        1 -> CustomScreen(
          viewModel = viewModel,
          onProfileClick = { viewModel.selectTab(if (isAdmin) 3 else 2) }
        )
        2 -> UserScreen(
          viewModel = viewModel,
          onNavigateToAuth = onNavigateToAuth
        )
      }"""

new_when = """      when (tab) {
        0 -> MatchesScreen(
          viewModel = viewModel,
          onMatchClick = onMatchClick,
          onProfileClick = { viewModel.selectTab(if (isAdmin) 3 else 2) }
        )
        1 -> CustomScreen(
          viewModel = viewModel,
          onProfileClick = { viewModel.selectTab(if (isAdmin) 3 else 2) }
        )
        2 -> if (isAdmin) AdminDashboardScreen(viewModel = viewModel) else UserScreen(viewModel = viewModel, onNavigateToAuth = onNavigateToAuth)
        3 -> UserScreen(viewModel = viewModel, onNavigateToAuth = onNavigateToAuth)
      }"""

# Re-read the exact when
when_content = """      when (tab) {
        0 -> MatchesScreen(
          viewModel = viewModel,
          onMatchClick = onMatchClick,
          onProfileClick = { viewModel.selectTab(2) }
        )
        1 -> CustomScreen(
          viewModel = viewModel,
          onProfileClick = { viewModel.selectTab(2) }
        )
        2 -> UserScreen(
          viewModel = viewModel,
          onNavigateToAuth = onNavigateToAuth
        )
      }"""

content = content.replace(when_content, new_when)

with open("app/src/main/java/com/example/ui/navigation/AppNavigation.kt", "w") as f:
    f.write(content)
