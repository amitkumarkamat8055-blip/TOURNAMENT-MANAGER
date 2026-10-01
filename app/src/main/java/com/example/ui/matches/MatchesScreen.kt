package com.example.ui.matches
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.widthIn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MatchItem
import com.example.data.model.isDailyMatch
import com.example.data.model.getEffectivePerKill
import com.example.data.model.displayDate
import com.example.ui.admin.CreateOrEditMatchDialog
import com.example.ui.admin.SendRoomIdDialog
import com.example.ui.components.AppHeader
import com.example.ui.components.AppSearchBar
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.MatchNumberBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.TournamentViewModel

@Composable
fun MatchesScreen(
  viewModel: TournamentViewModel,
  onMatchClick: (Long) -> Unit,
  onProfileClick: () -> Unit,
  onWalletClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val matches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val searchQuery by viewModel.matchSearchQuery.collectAsStateWithLifecycle()
  val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  val statuses = listOf("All", "Ongoing", "Upcoming", "Completed")
  var selectedTab by remember { mutableIntStateOf(0) }
  var dailyStatus by remember { mutableStateOf("All") }
  var weeklyStatus by remember { mutableStateOf("All") }
  var showCreateMatchDialog by remember { mutableStateOf(false) }
  var matchToEdit by remember { mutableStateOf<MatchItem?>(null) }
  var matchToDelete by remember { mutableStateOf<MatchItem?>(null) }
  var matchForRoomId by remember { mutableStateOf<MatchItem?>(null) }
  var matchForCandidates by remember { mutableStateOf<MatchItem?>(null) }

  val listState = androidx.compose.foundation.lazy.rememberLazyListState()

  val registrations by viewModel.allMatchRegistrations.collectAsStateWithLifecycle()

  if (showCreateMatchDialog) {
    CreateOrEditMatchDialog(
      existingMatch = null,
      defaultCategory = if (selectedTab == 0) "Daily" else "Weekly",
      onDismiss = { showCreateMatchDialog = false },
      onSave = { name, game, cat, fee, prize, maxP, date, time, format, map, rules, perKill, bannerUrl ->
        viewModel.createTournamentMatch(name, game, cat, fee, prize, maxP, date, time, format, map, rules, perKill, bannerUrl) {
          showCreateMatchDialog = false
        }
      }
    )
  }

  matchToEdit?.let { match ->
    CreateOrEditMatchDialog(
      existingMatch = match,
      onDismiss = { matchToEdit = null },
      onSave = { name, game, cat, fee, prize, maxP, date, time, format, map, rules, perKill, bannerUrl ->
        val finalName = if (cat.equals("Weekly", ignoreCase = true) && !name.contains("Weekly", ignoreCase = true)) {
          "Weekly $name"
        } else if (cat.equals("Daily", ignoreCase = true) && !name.contains("Daily", ignoreCase = true)) {
          "Daily $name"
        } else {
          name
        }
        viewModel.updateMatchDetails(
          match.id,
          fee,
          prize,
          maxP,
          date,
          time,
          rules,
          perKill,
          bannerUrl,
          name = finalName,
          gameTitle = game,
          format = format,
          mapName = map
        )
        matchToEdit = null
      }
    )
  }

  matchToDelete?.let { match ->
    AlertDialog(
      onDismissRequest = { matchToDelete = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = AlertRed,
          modifier = Modifier.size(28.dp)
        )
      },
      title = { Text("Delete Match?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to delete '${match.name}'? All candidate registrations, broadcast credentials, and match details will be permanently removed. This action cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteTournamentMatch(match.id)
            matchToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_match_button")
        ) {
          Text("Delete Match", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { matchToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  matchForRoomId?.let { match ->
    val regCount = registrations.count { it.matchId == match.id }
    SendRoomIdDialog(
      match = match,
      registeredCandidatesCount = regCount,
      onDismiss = { matchForRoomId = null },
      onBroadcast = { roomId, password ->
        viewModel.broadcastRoomCredentials(match.id, roomId, password) {
          matchForRoomId = null
        }
      }
    )
  }

  matchForCandidates?.let { match ->
    MatchCandidatesQuickDialog(
      match = match,
      viewModel = viewModel,
      onDismiss = { matchForCandidates = null }
    )
  }

  Scaffold(
    floatingActionButton = {
      if (isAdmin) {
        Box(
          modifier = Modifier
            .padding(bottom = 60.dp)
            .size(56.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
              )
            )
            .clickable { showCreateMatchDialog = true }
            .testTag("matches_tab_fab_add_match"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Match Room Profile",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    },
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .displayCutoutPadding()
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Section
      AppHeader(
        title = if (isAdmin) "Admin" else "Tournament Hub",
        subtitle = if (isAdmin) "Manage matches & tournaments" else "Join matches & win prize pools",
        walletBalance = userProfile.walletBalance,
        onProfileClick = onProfileClick,
        onWalletClick = onWalletClick,
        logoIcon = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.SportsEsports
      )

    // Daily and Weekly Match Section Tabs
    androidx.compose.material3.TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.primary,
      indicator = { tabPositions ->
        androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = MaterialTheme.colorScheme.primary
        )
      }
    ) {
      androidx.compose.material3.Tab(
        selected = selectedTab == 0,
        onClick = {
          selectedTab = 0
          viewModel.setSelectedStatus(dailyStatus)
        },
        text = { androidx.compose.material3.Text("Daily", style = MaterialTheme.typography.labelLarge) }
      )
      androidx.compose.material3.Tab(
        selected = selectedTab == 1,
        onClick = {
          selectedTab = 1
          viewModel.setSelectedStatus(weeklyStatus)
        },
        text = { androidx.compose.material3.Text("Weekly", style = MaterialTheme.typography.labelLarge) }
      )
    }

    // Status Filter Chips (All, Ongoing, Upcoming, Completed) placed under Daily & Weekly match sections
    val currentSectionStatus = if (selectedTab == 0) dailyStatus else weeklyStatus
    CategoryFilterRow(
      categories = statuses,
      selectedCategory = currentSectionStatus,
      onCategorySelected = { status ->
        if (selectedTab == 0) {
          dailyStatus = status
        } else {
          weeklyStatus = status
        }
        viewModel.setSelectedStatus(status)
      }
    )

    // Matches List (Exclusively Free Fire MAX tournaments)
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    val freeFireMatches = matches.filter {
      !it.gameTitle.contains("PUBG", ignoreCase = true) &&
      !it.gameTitle.contains("BGMI", ignoreCase = true) &&
      !it.gameTitle.contains("COD", ignoreCase = true) &&
      !it.gameTitle.contains("Valorant", ignoreCase = true) &&
      !it.name.contains("PUBG", ignoreCase = true) &&
      !it.name.contains("BGMI", ignoreCase = true) &&
      !it.name.contains("Apex", ignoreCase = true)
    }

    val dailyMatches = freeFireMatches.filter { it.name.contains("Daily", ignoreCase = true) || (!it.name.contains("Daily", ignoreCase = true) && !it.name.contains("Weekly", ignoreCase = true)) }
    val weeklyMatches = freeFireMatches.filter { it.name.contains("Weekly", ignoreCase = true) }
    
    val displayMatches = if (selectedTab == 0) dailyMatches else weeklyMatches

    if (displayMatches.isEmpty()) {
      EmptyMatchesView(
        isFiltered = searchQuery.isNotBlank() || selectedStatus != "All",
        sectionTitle = if (selectedTab == 0) "Daily" else "Weekly",
        isAdmin = isAdmin,
        onCreateMatch = { showCreateMatchDialog = true },
        onResetFilter = {
          viewModel.setMatchSearchQuery("")
          viewModel.setSelectedStatus("All")
        }
      )
    } else {
      @OptIn(
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.foundation.layout.ExperimentalLayoutApi::class
      )
      androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshMatches() },
        modifier = Modifier.fillMaxSize()
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .testTag("matches_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          itemsIndexed(
            items = displayMatches,
            key = { _, match -> match.id }
          ) { index, match ->
            Column {
              AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(durationMillis = 300)) + slideInVertically(initialOffsetY = { it / 4 })
              ) {
                MatchCard(
                  match = match,
                  isAdmin = isAdmin,
                  onCardClick = { onMatchClick(match.id) },
                  onJoinClick = { onMatchClick(match.id) },
                  onEditClick = { matchToEdit = match },
                  onDeleteClick = { matchToDelete = match },
                  onSendRoomClick = { matchForRoomId = match },
                  onViewCandidatesClick = { matchForCandidates = match }
                )
              }
            }
          }
        }
      }
    }
  }
}
}

@Composable
fun MatchCard(
  match: MatchItem,
  isAdmin: Boolean = false,
  onCardClick: () -> Unit,
  onJoinClick: () -> Unit,
  onEditClick: () -> Unit = {},
  onDeleteClick: () -> Unit = {},
  onSendRoomClick: () -> Unit = {},
  onViewCandidatesClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val progress = (match.totalPlayers.toFloat() / match.maxPlayers.toFloat()).coerceIn(0f, 1f)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable { onCardClick() }
      .testTag("match_card_${match.matchNumber}"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      1.dp,
      if (match.isJoined) SuccessGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. BANNER HEADER SPACE (Esports styling with Free Fire badge, match number and status)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(104.dp)
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF0E2517), // Deep tactical dark green
                Color(0xFF163C25), // Esports emerald green
                Color(0xFF101B14)  // Dark tactical slate
              )
            )
          )
      ) {
        if (match.bannerImageUrl.isNotBlank()) {
          AsyncImage(
            model = match.bannerImageUrl,
            contentDescription = "Match Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    Color.Black.copy(alpha = 0.45f),
                    Color.Black.copy(alpha = 0.72f)
                  )
                )
              )
          )
        } else {
          // Decorative background esports watermark
          Icon(
            imageVector = Icons.Default.SportsEsports,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.07f),
            modifier = Modifier
              .size(110.dp)
              .align(Alignment.BottomEnd)
              .padding(end = 12.dp, bottom = 4.dp)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Top Row inside Banner: Match # Badge, Free Fire MAX badge, Status Badge, Admin actions (Edit & Delete)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.weight(1f, fill = false)
            ) {
              MatchNumberBadge(number = match.matchNumber)

              // Prominent Free Fire Game Badge
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE65100).copy(alpha = 0.32f),
                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "FREE FIRE MAX",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 9.5.sp,
                      letterSpacing = 0.4.sp
                    ),
                    color = Color(0xFFFFCC80)
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              StatusBadge(status = match.status)
              if (isAdmin) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.Black.copy(alpha = 0.65f),
                  border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                  ) {
                    IconButton(
                      onClick = onEditClick,
                      modifier = Modifier.size(26.dp).testTag("btn_edit_match_${match.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Match",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                    IconButton(
                      onClick = onDeleteClick,
                      modifier = Modifier.size(26.dp).testTag("btn_delete_match_${match.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Match",
                        tint = AlertRed,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          // Bottom Row inside Banner: Map & Format Frosted Pill
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${match.mapName}  •  ${match.format}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      // 2. CARD BODY CONTENT
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        // SCHEDULE: Simple, clean, unified Date and Time bar
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Day
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Event,
                contentDescription = "Match Day",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "DAY",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = match.displayDate,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  ),
                  color = SuccessGreen,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            )

            // Time
            Row(
              modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Match Time",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "TIME",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = match.time,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  ),
                  color = TrophyGold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Match Title
        Text(
          text = match.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Grid (Entry Fee, Prize Pool, Players, Rank)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Entry Fee
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Entry Fee",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (match.entryFee == 0) "FREE" else "₹${match.entryFee}",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Black,
                  color = if (match.entryFee == 0) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
              )
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            // Prize Pool
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Prize Pool",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "₹${match.prizePool}",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Black,
                  color = TrophyGold
                )
              )
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            // Players
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Players",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${match.totalPlayers}/${match.maxPlayers}",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              )
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            )

            // 4th Metric: ONLY in Daily Matches show 'Per Kill', in Weekly show 'Level'
            if (match.isDailyMatch) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "Per Kill",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = TrophyGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "₹${match.getEffectivePerKill()}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = TrophyGold
                  ),
                  maxLines = 1
                )
              }
            } else {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "Level",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = match.rankRequirement,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SuccessGreen
                  ),
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Player Fill Progress bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (progress > 0.85f) AlertRed else SuccessGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${(progress * 100).toInt()}% Full",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Admin Action Row: Room ID & View Candidates (Admin only - no Join Match)
        if (isAdmin) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onSendRoomClick,
              modifier = Modifier.weight(1f).height(42.dp).testTag("btn_send_room_${match.id}"),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Room ID/Pass", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
              onClick = onViewCandidatesClick,
              modifier = Modifier.weight(1f).height(42.dp).testTag("btn_view_candidates_${match.id}"),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Candidates", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }
        } else {
          Spacer(modifier = Modifier.height(14.dp))

          // Action Button: JOIN MATCH / JOINED (User only)
          if (match.isJoined) {
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = com.example.ui.theme.BadgeSuccessBg,
              border = BorderStroke(1.dp, com.example.ui.theme.BadgeSuccessText.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick() }
                .testTag("match_joined_indicator_${match.matchNumber}")
            ) {
              Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (match.isRoomBroadcasted) Icons.Default.Campaign else Icons.Default.CheckCircle,
                  contentDescription = "Joined",
                  tint = com.example.ui.theme.BadgeSuccessText,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (match.isRoomBroadcasted) "VIEW DETAILS • ROOM BROADCASTED ✓" else "REGISTERED • VIEW DETAILS",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = com.example.ui.theme.BadgeSuccessText
                )
              }
            }
          } else {
            Button(
              onClick = onJoinClick,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("join_match_button_${match.matchNumber}"),
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SuccessGreen,
                contentColor = Color.White
              ),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "JOIN MATCH",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EmptyMatchesView(
  isFiltered: Boolean,
  sectionTitle: String = "Daily",
  isAdmin: Boolean = false,
  onCreateMatch: () -> Unit = {},
  onResetFilter: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.SportsEsports,
        contentDescription = "No Matches",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = if (isFiltered) "No Matches Found" else "No $sectionTitle Matches Available",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (isFiltered)
        "Try changing your search query or selected category filter."
      else
        "No $sectionTitle matches have been created yet. When the admin hosts a $sectionTitle match, it will appear here.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    if (isFiltered) {
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(
        onClick = onResetFilter,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("reset_match_filter_button")
      ) {
        Text("Clear Filters")
      }
    } else if (isAdmin) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onCreateMatch,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("empty_view_create_match_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create $sectionTitle Match")
      }
    }
  }
}

@Composable
fun MatchCandidatesQuickDialog(
    match: MatchItem,
    viewModel: TournamentViewModel,
    onDismiss: () -> Unit
) {
    val registrations by viewModel.allMatchRegistrations.collectAsStateWithLifecycle()
    val matchRegs = registrations.filter { it.matchId == match.id }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Registered Candidates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${match.name} • ${matchRegs.size}/${match.maxPlayers} Joined",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (matchRegs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No candidates registered yet for this match.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matchRegs.size) { idx ->
                            val reg = matchRegs[idx]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reg.candidateName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Game UID: ${reg.gameUid}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = reg.phoneOrEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FilterChip(
                                        selected = reg.paymentStatus == "Paid",
                                        onClick = {
                                            val newStatus = if (reg.paymentStatus == "Paid") "Pending" else "Paid"
                                            viewModel.updateRegistrationPaymentStatus(reg.id, newStatus)
                                        },
                                        label = {
                                            Text(
                                                text = reg.paymentStatus,
                                                fontWeight = FontWeight.Bold,
                                                color = if (reg.paymentStatus == "Paid") SuccessGreen else AlertRed
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (reg.paymentStatus == "Paid") Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = if (reg.paymentStatus == "Paid") SuccessGreen else AlertRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

