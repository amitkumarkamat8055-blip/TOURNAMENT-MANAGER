package com.example.ui.navigation

import com.example.ui.admin.AdminDashboardScreen


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AuthStatus
import com.example.ui.auth.AuthScreen
import com.example.ui.components.BottomNavBar
import com.example.ui.custom.CustomScreen
import com.example.ui.matches.MatchDetailsScreen
import com.example.ui.matches.MatchesScreen
import com.example.ui.user.UserScreen
import com.example.ui.viewmodel.TournamentViewModel
import com.example.ui.viewmodel.UiEvent

sealed class Screen(val route: String) {
  object Auth : Screen("auth")
  object Main : Screen("main")
  object Wallet : Screen("wallet")
  object MatchDetails : Screen("match_details/{matchId}") {
    fun createRoute(matchId: Long) = "match_details/$matchId"
  }
}

@Composable
fun AppNavigation(
  viewModel: TournamentViewModel,
  modifier: Modifier = Modifier
) {
  val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()

  when (authStatus) {
    is AuthStatus.Initializing -> {
      Box(
        modifier = modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.SportsEsports,
              contentDescription = "Loading Arena",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(36.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
          )
        }
      }
    }
    is AuthStatus.LoggedOut -> {
      AuthScreen(
        viewModel = viewModel,
        onAuthSuccess = {
          // Handled via ViewModel state update
        },
        modifier = modifier
      )
    }
    is AuthStatus.LoggedIn -> {
      AuthenticatedAppContent(
        viewModel = viewModel,
        modifier = modifier
      )
    }
  }
}

@Composable
fun AuthenticatedAppContent(
  viewModel: TournamentViewModel,
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = androidx.compose.ui.platform.LocalContext.current
  val allMatches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  // App-level notification listener strictly for matches the candidate is registered for
  LaunchedEffect(allMatches, isAdmin) {
    if (isAdmin) return@LaunchedEffect
    allMatches.filter { it.isJoined }.forEach { match ->
      if (match.isRoomBroadcasted && match.roomId.isNotBlank() && match.roomPassword.isNotBlank()) {
        val key = "match_broadcast_${match.id}_${match.roomId}_${match.roomPassword}"
        val notified = com.example.worker.NotificationManagerHelper.showImmediateNotificationOnce(
          context,
          key = key,
          title = "Room ID & Password: ${match.name}",
          message = "Room ID: ${match.roomId} | Password: ${match.roomPassword}. Open Free Fire MAX to enter now!"
        )
        if (notified) {
          snackbarHostState.showSnackbar("Room ID & Password released for ${match.name}!")
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
      when (event) {
        is UiEvent.ShowSnackbar -> {
          snackbarHostState.showSnackbar(event.message)
        }
        is UiEvent.NavigateToMatchDetails -> {
          navController.navigate(Screen.MatchDetails.createRoute(event.matchId))
        }
      }
    }
  }

  NavHost(
    navController = navController,
    startDestination = Screen.Main.route,
    modifier = modifier
  ) {
    composable(Screen.Main.route) {
      val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
      MainContainerScreen(
        viewModel = viewModel,
        isAdmin = isAdmin,
        snackbarHostState = snackbarHostState,
        onMatchClick = { matchId ->
          navController.navigate(Screen.MatchDetails.createRoute(matchId))
        },
        onNavigateToAuth = {
          viewModel.logout()
        },
        onWalletClick = {
          navController.navigate(Screen.Wallet.route)
        }
      )
    }

    composable(
      route = Screen.MatchDetails.route,
      arguments = listOf(
        navArgument("matchId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
      MatchDetailsScreen(
        matchId = matchId,
        viewModel = viewModel,
        onBackClick = { navController.popBackStack() }
      )
    }
    
    composable(Screen.Wallet.route) {
      com.example.ui.wallet.WalletScreen(
        viewModel = viewModel,
        onBackClick = { navController.popBackStack() }
      )
    }
  }
}



@Composable
fun MainContainerScreen(
  viewModel: TournamentViewModel,
  isAdmin: Boolean,
  snackbarHostState: SnackbarHostState,
  onMatchClick: (Long) -> Unit,
  onNavigateToAuth: () -> Unit,
  onWalletClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

  Scaffold(
    bottomBar = {
      BottomNavBar(
        selectedTab = selectedTab,
        onTabSelected = { viewModel.selectTab(it) },
        isAdmin = isAdmin
      )
    },
    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize().imePadding()
  ) { innerPadding ->
    AnimatedContent(
      targetState = selectedTab,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "TabContentAnimation",
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = innerPadding.calculateBottomPadding())
    ) { tab ->
      when (tab) {
        0 -> MatchesScreen(
          viewModel = viewModel,
          onMatchClick = onMatchClick,
          onProfileClick = { viewModel.selectTab(2) },
          onWalletClick = onWalletClick
        )
        1 -> CustomScreen(
          viewModel = viewModel,
          onProfileClick = { viewModel.selectTab(2) },
          onWalletClick = onWalletClick
        )
        2 -> UserScreen(
          viewModel = viewModel,
          onNavigateToAuth = onNavigateToAuth,
          onWalletClick = onWalletClick
        )
        else -> MatchesScreen(
          viewModel = viewModel,
          onMatchClick = onMatchClick,
          onProfileClick = { viewModel.selectTab(2) },
          onWalletClick = onWalletClick
        )
      }
    }
  }
}

