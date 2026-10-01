package com.example.ui.custom
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.example.data.model.CustomTournament
import com.example.data.model.CustomProfile
import com.example.data.model.CustomProfileApplication
import com.example.data.model.formatMatchDisplayDate
import com.example.ui.components.AppHeader
import com.example.ui.components.AppSearchBar
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.TournamentViewModel

fun isProfileExpired(day: String, time: String): Boolean {
    val format = java.text.SimpleDateFormat("d MMM h:mm a", java.util.Locale.US)
    val currentCal = java.util.Calendar.getInstance()
    val currentYear = currentCal.get(java.util.Calendar.YEAR)
    
    try {
        val trimmed = day.trim()
        val resolvedDay = when {
            trimmed.startsWith("Today, ", ignoreCase = true) -> trimmed.substringAfter("Today, ").trim()
            trimmed.startsWith("Tomorrow, ", ignoreCase = true) -> trimmed.substringAfter("Tomorrow, ").trim()
            trimmed.equals("today", ignoreCase = true) -> java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(currentCal.time)
            trimmed.equals("tomorrow", ignoreCase = true) -> {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(cal.time)
            }
            else -> trimmed
        }
        val date = format.parse("$resolvedDay $time")
        if (date != null) {
            val profileCal = java.util.Calendar.getInstance()
            profileCal.time = date
            profileCal.set(java.util.Calendar.YEAR, currentYear)
            
            // Allow a 60-minute match completion grace period before marking expired
            return profileCal.timeInMillis + (60 * 60 * 1000L) < currentCal.timeInMillis
        }
    } catch (_: Exception) {
    }
    return false
}

@Composable
fun CustomScreen(
  viewModel: TournamentViewModel,
  onProfileClick: () -> Unit,
  onWalletClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val customTournaments by viewModel.filteredCustomTournaments.collectAsStateWithLifecycle()
  val firestoreProfiles by viewModel.firestoreCustomProfiles.collectAsStateWithLifecycle()
  val searchQuery by viewModel.customSearchQuery.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val myAppliedCustomProfiles by viewModel.myAppliedCustomProfiles.collectAsStateWithLifecycle()
  val myCustomProfileApplications by viewModel.myCustomProfileApplications.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val allAdminApps by viewModel.allCustomProfileApplicationsForAdmin.collectAsStateWithLifecycle()
  val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
  val activeAccount = (authStatus as? com.example.data.model.AuthStatus.LoggedIn)?.account

  var showCreateDialog by remember { mutableStateOf(false) }
  var profileToDelete by remember { mutableStateOf<CustomProfile?>(null) }
  var profileToEdit by remember { mutableStateOf<CustomProfile?>(null) }
  var profileToSubmit by remember { mutableStateOf<CustomProfile?>(null) }
  var profileToJoin by remember { mutableStateOf<CustomProfile?>(null) }
  var selectedRoomForCandidates by remember { mutableStateOf<CustomProfile?>(null) }
  var selectedRoomForSendingCredentials by remember { mutableStateOf<CustomProfile?>(null) }
  var profileToShowDetails by remember { mutableStateOf<CustomProfile?>(null) }
  var expiredProfileToManage by remember { mutableStateOf<CustomProfile?>(null) }
  var adminInspectionCandidatesProfile by remember { mutableStateOf<CustomProfile?>(null) }
  var adminInspectionResultsProfile by remember { mutableStateOf<CustomProfile?>(null) }
  
  var selectedFreeFireFilter by remember { mutableStateOf("Custom") }
  var myRoomSelectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
  
  val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

  fun isProfileHost(profile: CustomProfile): Boolean {
    if (isAdmin) return true
    val activeGameUid = activeAccount?.gameUid?.trim()?.takeIf { it.isNotBlank() } ?: userProfile.uid.trim()
    val activePhone = activeAccount?.phone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() } ?: ""
    val activeUsername = activeAccount?.username?.trim()?.takeIf { it.isNotBlank() } ?: userProfile.name.trim()

    if (activeGameUid.isNotBlank() && (
        profile.uid.equals(activeGameUid, ignoreCase = true) ||
        profile.hostUid.equals(activeGameUid, ignoreCase = true) ||
        profile.hostGameUid.equals(activeGameUid, ignoreCase = true)
    )) {
      return true
    }
    if (currentAuthUid.isNotBlank() && profile.hostUid.isNotBlank() && profile.hostUid == currentAuthUid) {
      return true
    }
    if (activePhone.isNotBlank() && profile.hostPhone.filter { it.isDigit() }.isNotBlank() && profile.hostPhone.filter { it.isDigit() } == activePhone) {
      return true
    }
    if (activeUsername.isNotBlank() && (profile.hostActualName.equals(activeUsername, ignoreCase = true) || profile.name.equals(activeUsername, ignoreCase = true))) {
      return true
    }
    return false
  }

  val filteredProfiles = remember(firestoreProfiles, searchQuery, selectedFreeFireFilter, currentAuthUid, userProfile, activeAccount, myRoomSelectedTab, isAdmin) {
    var result = firestoreProfiles
    
    if (searchQuery.isNotBlank()) {
      result = result.filter { 
         it.name.contains(searchQuery, ignoreCase = true) || 
         it.uid.contains(searchQuery, ignoreCase = true) 
      }
    }
    
    when (selectedFreeFireFilter) {
       "Custom" -> result = result.filter { it.category.isBlank() || it.category.equals("Custom", ignoreCase = true) }
       "BR" -> result = result.filter { it.category.equals("BR", ignoreCase = true) }
       "Region" -> result = emptyList() // Handled via UI empty state directly
       "My Room" -> {
           result = result.filter { isProfileHost(it) }
           when (myRoomSelectedTab) {
               1 -> result = result.filter { it.category.isBlank() || it.category.equals("Custom", ignoreCase = true) }
               2 -> result = result.filter { it.category.equals("BR", ignoreCase = true) }
               else -> { /* 0 = All rooms hosted by user */ }
           }
       }
    }
    
    result
  }
  
  androidx.compose.runtime.LaunchedEffect(firestoreProfiles, currentAuthUid) {
      if (currentAuthUid.isNotBlank()) {
          val myProfiles = firestoreProfiles.filter { isProfileHost(it) }
          val currentTime = System.currentTimeMillis()
          for (profile in myProfiles) {
              // Never trigger expiration if profile was created in the last 4 hours
              if (profile.createdAt > 0 && currentTime - profile.createdAt < 4 * 60 * 60 * 1000L) {
                  continue
              }
              if (isProfileExpired(profile.day, profile.time)) {
                  expiredProfileToManage = profile
                  break 
              }
          }
      }
  }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    viewModel.fetchCustomProfiles()
  }

  androidx.compose.runtime.LaunchedEffect(isAdmin) {
    if (isAdmin) {
      viewModel.fetchAllCustomProfileApplicationsForAdmin()
    }
  }

  Scaffold(
    floatingActionButton = {
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
          .clickable { showCreateDialog = true }
          .testTag("custom_tab_fab_add"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Custom Profile",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
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
      // Top section matching Sketch: CUSTOM TAB Title
      AppHeader(
        title = "Custom Rooms",
        subtitle = "Community hosted custom rooms & profiles",
        walletBalance = userProfile.walletBalance,
        onProfileClick = onProfileClick,
        onWalletClick = onWalletClick
      )

      // Admin Mode Banner for Admin User (RdLDfdXOeEazslnOpYWQLNpmKag1)
      if (isAdmin) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "ADMIN PANEL CONTROL",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Admin: RdLDfdXOeEazslnOpYWQLNpmKag1",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }

      // Search Bar
      AppSearchBar(
        query = searchQuery,
        onQueryChange = { viewModel.setCustomSearchQuery(it) },
        placeholder = "Search custom profile, UID, region...",
        modifier = Modifier.padding(bottom = 6.dp)
      )

      // Filters
      val screenWidth = LocalConfiguration.current.screenWidthDp
      val filterHorizontalPadding = when {
        screenWidth >= 840 -> 24.dp
        screenWidth >= 600 -> 20.dp
        else -> 16.dp
      }

      Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = filterHorizontalPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            androidx.compose.material3.FilterChip(
              selected = selectedFreeFireFilter == "Custom",
              onClick = { selectedFreeFireFilter = "Custom" },
              label = { Text("Custom") },
              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                 containerColor = MaterialTheme.colorScheme.surfaceVariant,
                 selectedContainerColor = SuccessGreen,
                 selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(16.dp)
            )
            androidx.compose.material3.FilterChip(
              selected = selectedFreeFireFilter == "BR",
              onClick = { selectedFreeFireFilter = "BR" },
              label = { Text("BR") },
              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                 containerColor = MaterialTheme.colorScheme.surfaceVariant,
                 selectedContainerColor = SuccessGreen,
                 selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(16.dp)
            )
            androidx.compose.material3.FilterChip(
              selected = selectedFreeFireFilter == "Region",
              onClick = { selectedFreeFireFilter = "Region" },
              label = { Text("Region") },
              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                 containerColor = MaterialTheme.colorScheme.surfaceVariant,
                 selectedContainerColor = SuccessGreen,
                 selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(16.dp)
            )
            androidx.compose.material3.FilterChip(
              selected = selectedFreeFireFilter == "My Room",
              onClick = { selectedFreeFireFilter = "My Room" },
              label = { Text("My Room") },
              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                 containerColor = MaterialTheme.colorScheme.surfaceVariant,
                 selectedContainerColor = SuccessGreen,
                 selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(16.dp)
            )
          }
      }

      if (selectedFreeFireFilter == "My Room") {
        androidx.compose.material3.TabRow(
          selectedTabIndex = myRoomSelectedTab,
          containerColor = MaterialTheme.colorScheme.background,
          contentColor = MaterialTheme.colorScheme.primary,
          indicator = { tabPositions ->
            androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[myRoomSelectedTab]),
              color = MaterialTheme.colorScheme.primary
            )
          }
        ) {
          androidx.compose.material3.Tab(
            selected = myRoomSelectedTab == 0,
            onClick = { myRoomSelectedTab = 0 },
            text = { Text("All", style = MaterialTheme.typography.labelLarge) }
          )
          androidx.compose.material3.Tab(
            selected = myRoomSelectedTab == 1,
            onClick = { myRoomSelectedTab = 1 },
            text = { Text("Custom", style = MaterialTheme.typography.labelLarge) }
          )
          androidx.compose.material3.Tab(
            selected = myRoomSelectedTab == 2,
            onClick = { myRoomSelectedTab = 2 },
            text = { Text("BR", style = MaterialTheme.typography.labelLarge) }
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Custom Profile Cards
      if (selectedFreeFireFilter == "Region") {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                  Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                  Spacer(modifier = Modifier.height(16.dp))
                  Text("Region Filter Coming Soon", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("This feature will be available in a future update.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
          }
      } else if (filteredProfiles.isEmpty()) {
        EmptyCustomTournamentsView(
          isFiltered = searchQuery.isNotBlank(),
          onCreateClick = { showCreateDialog = true }
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("custom_tournaments_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          itemsIndexed(
            items = filteredProfiles,
            key = { _, item -> item.id }
          ) { _, profile ->
            AnimatedVisibility(
              visible = true,
              enter = fadeIn(tween(durationMillis = 250)) +
                  slideInVertically(initialOffsetY = { it / 4 })
            ) {
              val isMyRoomFilter = selectedFreeFireFilter == "My Room"
              val isUserHost = isProfileHost(profile)
              // Delete and Edit icons should NEVER appear in Custom filter for any user or admin
              val showEditAndDelete = isMyRoomFilter && isUserHost
              val isHostOwnRoom = isMyRoomFilter && isUserHost && !isAdmin

              val roomApplications = remember(allAdminApps, myCustomProfileApplications, myAppliedCustomProfiles, profile.id) {
                (allAdminApps + myCustomProfileApplications + myAppliedCustomProfiles).distinctBy { it.id }.filter { it.profileId == profile.id }
              }
              val acceptedCandidate = remember(roomApplications) {
                roomApplications.firstOrNull {
                  it.status.equals("Accepted", ignoreCase = true) ||
                  it.status.equals("Paid", ignoreCase = true) ||
                  it.status.equals("Result Submitted", ignoreCase = true) ||
                  it.status.startsWith("Prize Sent", ignoreCase = true)
                }
              }
              val hasSubmittedResult = remember(roomApplications) {
                roomApplications.any { it.isResultSubmitted || it.resultScreenshot.isNotBlank() }
              }
              val hasSubmittedReport = remember(roomApplications) {
                roomApplications.any { it.isReported || it.reportReason.isNotBlank() || it.reportDescription.isNotBlank() }
              }
              val isRoomJoined = myAppliedCustomProfiles.any { it.profileId == profile.id } || viewModel.hasAppliedToCustomProfile(profile.id)

              CustomProfileCard(
                profile = profile,
                onJoinClick = { 
                    if (isUserHost) {
                        selectedFreeFireFilter = "My Room"
                    } else if (profile.category == "BR" || isRoomJoined) {
                        profileToShowDetails = profile
                    } else {
                        profileToJoin = profile 
                    }
                },
                onViewCandidatesClick = if (isHostOwnRoom) { { selectedRoomForCandidates = profile } } else null,
                onSendCredentialsClick = if (isHostOwnRoom) { { selectedRoomForSendingCredentials = profile } } else null,
                onSubmit = if (isHostOwnRoom) { { profileToSubmit = profile } } else null,
                onEdit = if (showEditAndDelete) { { profileToEdit = profile; showCreateDialog = true } } else null,
                onDelete = if (showEditAndDelete) { { profileToDelete = profile } } else null,
                isJoined = isRoomJoined,
                isOwnRoom = isHostOwnRoom,
                isHostUser = isUserHost,
                isAdmin = isAdmin,
                candidateCountBadge = roomApplications.size,
                acceptedCandidateName = acceptedCandidate?.candidateName,
                hasSubmittedResult = hasSubmittedResult,
                hasSubmittedReport = hasSubmittedReport,
                onAdminViewCandidates = { adminInspectionCandidatesProfile = profile },
                onAdminViewResultsAndReports = { adminInspectionResultsProfile = profile }
              )
            }
          }
        }
      }
    }
  }

  // Match Details Dialog (BR Room Info)
  if (profileToShowDetails != null) {
      val userApp = myAppliedCustomProfiles.find { it.profileId == profileToShowDetails!!.id }
          ?: allAdminApps.find { it.profileId == profileToShowDetails!!.id && (it.uid == userProfile.uid || it.applicantUid == currentAuthUid) }
      MatchDetailsDialog(
          profile = profileToShowDetails!!,
          isAdmin = isAdmin,
          userApplication = userApp,
          onDismiss = { profileToShowDetails = null },
          onProceed = { 
              profileToJoin = profileToShowDetails
              profileToShowDetails = null 
          }
      )
  }

  // Join Room Dialog
  if (profileToJoin != null) {
    ApplyCandidateDialog(
      profile = profileToJoin!!,
      defaultName = activeAccount?.username?.takeIf { it.isNotBlank() } ?: userProfile.name,
      defaultUid = activeAccount?.gameUid?.takeIf { it.isNotBlank() } ?: userProfile.uid,
      defaultPhone = activeAccount?.phone ?: "",
      onDismiss = { profileToJoin = null },
      onSubmit = { name, phone, uid, level, rank ->
        viewModel.submitCustomProfileApplication(profileToJoin!!, name, phone, uid, level, rank)
        profileToJoin = null
      }
    )
  }

  // Create Profile / Tournament Modal
  if (showCreateDialog) {
    CreateProfileTournamentDialog(
      defaultUid = userProfile.uid,
      defaultRegion = userProfile.region,
      profileToEdit = profileToEdit,
      onDismiss = { 
          showCreateDialog = false 
          profileToEdit = null
      },
      onCreate = { name, uid, level, payout, prizePool, perKill, totalPlayers, category, game, day, time, type, mode, gun, imageUriStr ->
        if (profileToEdit != null) {
            viewModel.updateCustomProfile(
              profileId = profileToEdit!!.id,
              name = name,
              uid = uid,
              level = level,
              payout = payout,
              prizePool = prizePool,
              perKill = perKill,
              totalPlayers = totalPlayers,
              category = category,
              game = game,
              day = day,
              time = time,
              type = type,
              mode = mode,
              gun = gun,
              imageUriStr = imageUriStr,
              existingImageUrl = profileToEdit!!.imageUrl,
              onSuccess = { 
                  showCreateDialog = false 
                  profileToEdit = null
              }
            )
        } else {
            viewModel.createCustomProfile(
              name = name,
              uid = uid,
              level = level,
              payout = payout,
              prizePool = prizePool,
              perKill = perKill,
              totalPlayers = totalPlayers,
              category = category,
              game = game,
              day = day,
              time = time,
              type = type,
              mode = mode,
              gun = gun,
              imageUriStr = imageUriStr,
              onSuccess = { 
                  showCreateDialog = false
                  viewModel.fetchCustomProfiles()
              }
            )
        }
      }
    )
  }

  // Delete Tournament Confirmation
  profileToDelete?.let { profile ->
    AlertDialog(
      onDismissRequest = { profileToDelete = null },
      title = { Text("Delete Custom Profile") },
      text = { Text("Are you sure you want to remove \"${profile.name}\"?") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCustomProfile(profile.id)
            profileToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
          modifier = Modifier.testTag("confirm_delete_custom_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { profileToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Expired Profile Management Dialog
  expiredProfileToManage?.let { profile ->
    AlertDialog(
      onDismissRequest = { /* Forced Action */ },
      title = { Text("Room Expired") },
      text = { Text("Your room profile \"${profile.name}\" has expired. Please edit it to choose a new date and time, or it will be automatically deleted.") },
      confirmButton = {
        Button(
          onClick = {
            profileToEdit = profile
            showCreateDialog = true
            expiredProfileToManage = null
          }
        ) {
          Text("Edit Profile")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            viewModel.deleteCustomProfile(profile.id)
            expiredProfileToManage = null
          }
        ) {
          Text("Delete Profile", color = AlertRed)
        }
      },
      properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
  }

  if (profileToSubmit != null) {
      if (profileToSubmit!!.category == "BR") {
          BRMatchResultDialog(
              profile = profileToSubmit!!,
              onDismiss = { profileToSubmit = null },
              onSubmit = { profileToSubmit = null }
          )
      } else {
          SubmitSummaryDialog(
              profile = profileToSubmit!!,
              onDismiss = { profileToSubmit = null }
          )
      }
  }

  if (selectedRoomForCandidates != null) {
      val appsForRoom = remember(allAdminApps, myCustomProfileApplications, selectedRoomForCandidates) {
          (allAdminApps + myCustomProfileApplications).distinctBy { it.id }.filter { it.profileId == selectedRoomForCandidates!!.id }
      }
      com.example.ui.user.RoomCandidatesDialog(
          profile = selectedRoomForCandidates!!,
          applications = appsForRoom,
          onDismiss = { selectedRoomForCandidates = null },
          onUpdateStatus = { appId, profileId, status -> viewModel.updateApplicationStatus(appId, profileId, status) },
          onDelete = { appId -> viewModel.deleteCustomProfileApplication(appId) },
          onSendCandidateCredentials = { appId, rId, rPass -> viewModel.sendCandidateCredentials(appId, rId, rPass) },
          onHostPay = { appId -> viewModel.updateHostPayment(appId, true) },
          onSubmitResult = { appId, sUri, wName, wUid -> viewModel.submitMatchResult(appId, sUri, wName, wUid, "Host") },
          onSubmitReport = { appId, rReasons, rDesc, mUri -> viewModel.submitMatchReport(appId, rReasons, rDesc, mUri) }
      )
  }

  if (adminInspectionCandidatesProfile != null) {
      val roomApps = (allAdminApps + myCustomProfileApplications + myAppliedCustomProfiles)
          .distinctBy { it.id }
          .filter { it.profileId == adminInspectionCandidatesProfile!!.id }
      AdminCandidatesInspectionDialog(
          profile = adminInspectionCandidatesProfile!!,
          applications = roomApps,
          onDismiss = { adminInspectionCandidatesProfile = null },
          onUpdateStatus = { appId, profileId, status ->
              viewModel.updateApplicationStatus(appId, profileId, status)
          },
          onDelete = { appId ->
              viewModel.deleteCustomProfileApplication(appId)
          },
          onSendCredentials = { appId, rId, rPass ->
              viewModel.sendCandidateCredentials(appId, rId, rPass)
          },
          onUpdateHostPayment = { appId, paid ->
              viewModel.updateHostPayment(appId, paid)
          },
          onUpdateCandidatePayment = { appId, profileId, status ->
              viewModel.updateApplicationStatus(appId, profileId, status)
          }
      )
  }

  if (adminInspectionResultsProfile != null) {
      val roomApps = (allAdminApps + myCustomProfileApplications + myAppliedCustomProfiles)
          .distinctBy { it.id }
          .filter { it.profileId == adminInspectionResultsProfile!!.id }
      AdminResultsAndReportsDialog(
          profile = adminInspectionResultsProfile!!,
          applications = roomApps,
          onDismiss = { adminInspectionResultsProfile = null },
          onSendWinningAmount = { app, winnerUid, amount ->
              viewModel.sendCustomRoomWinningAmount(app, winnerUid, amount)
          },
          onDismissReport = { app ->
              viewModel.submitMatchReport(app.id, "", "", "")
          },
          onRefundOrCredit = { targetUid, amount, reason ->
              viewModel.creditOrRefundCustomRoomAmount(targetUid, amount, reason)
          },
          onUpdateHostPayment = { appId, paid ->
              viewModel.updateHostPayment(appId, paid)
          }
      )
  }

  if (selectedRoomForSendingCredentials != null) {
      val credentialsKeyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
      val credentialsFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
      var roomIdInput by remember { mutableStateOf("") }
      var roomPassInput by remember { mutableStateOf("") }
      androidx.compose.material3.AlertDialog(
          onDismissRequest = {
              credentialsKeyboardController?.hide()
              credentialsFocusManager.clearFocus()
              selectedRoomForSendingCredentials = null
          },
          title = { Text("Send Room Details") },
          text = {
              Column {
                  Text("Enter Room ID and Password to send to all Accepted candidates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(16.dp))
                  OutlinedTextField(
                      value = roomIdInput,
                      onValueChange = { roomIdInput = it },
                      label = { Text("Room ID") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth()
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedTextField(
                      value = roomPassInput,
                      onValueChange = { roomPassInput = it },
                      label = { Text("Password") },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth()
                  )
              }
          },
          confirmButton = {
              Button(
                  onClick = {
                      if (roomIdInput.isNotBlank() && roomPassInput.isNotBlank()) {
                          credentialsKeyboardController?.hide()
                          credentialsFocusManager.clearFocus()
                          viewModel.sendRoomCredentials(
                              profileId = selectedRoomForSendingCredentials!!.id,
                              roomId = roomIdInput,
                              roomPassword = roomPassInput
                          )
                          selectedRoomForSendingCredentials = null
                      }
                  },
                  enabled = roomIdInput.isNotBlank() && roomPassInput.isNotBlank()
              ) {
                  Text("Send")
              }
          },
          dismissButton = {
              androidx.compose.material3.TextButton(
                  onClick = {
                      credentialsKeyboardController?.hide()
                      credentialsFocusManager.clearFocus()
                      selectedRoomForSendingCredentials = null
                  }
              ) {
                  Text("Cancel")
              }
          }
      )
  }
}

@Composable
fun CustomTournamentCard(
  tournament: CustomTournament,
  cardIndex: Int,
  onJoinToggle: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("custom_card_$cardIndex"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      1.dp,
      if (tournament.isJoined) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      // Header: Number Badge (1, 2, 3...) & Region/Host
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Circular Card Number Badge as in hand-drawn design
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "$cardIndex",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = tournament.name,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "UID: ${tournament.hostUid}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                  onClick = {
                      clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(tournament.hostUid))
                      android.widget.Toast.makeText(context, "UID copied", android.widget.Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(16.dp)
              ) {
                  Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        if (tournament.isHostUser) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(32.dp)
              .testTag("delete_custom_$cardIndex")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Tournament",
              tint = AlertRed.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Custom Profile Info Grid
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Region
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Region",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = tournament.region,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NeonCyan
              ),
              maxLines = 1
            )
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(24.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
          )

          // Entry Fee
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Entry Fee",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (tournament.entryFee == 0) "FREE" else "₹${tournament.entryFee}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                color = if (tournament.entryFee == 0) SuccessGreen else MaterialTheme.colorScheme.onSurface
              )
            )
          }

          Box(
            modifier = Modifier
              .width(1.dp)
              .height(24.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
          )

          // Prize
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Prize",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "₹${tournament.prize}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                color = TrophyGold
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Match Info Description
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = tournament.matchInfo,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${tournament.currentPlayers}/${tournament.maxPlayers} Joined",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = onJoinToggle,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (tournament.isJoined) SuccessGreen else MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("custom_join_button_$cardIndex")
        ) {
          if (tournament.isJoined) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("JOINED", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          } else {
            Text("JOIN ROOM", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }
  }
}

@Composable
fun EmptyCustomTournamentsView(
  isFiltered: Boolean,
  onCreateClick: () -> Unit,
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
        .size(68.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.MilitaryTech,
        contentDescription = "No custom tournaments",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp)
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = if (isFiltered) "No Matching Custom Rooms" else "No Custom Profiles Created",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (isFiltered)
        "Try another search term."
      else
        "Tap the '+' floating button below to host your own custom tournament card!",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(
      onClick = onCreateClick,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier = Modifier.testTag("empty_state_create_custom_button")
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("CREATE PROFILE")
    }
  }
}

@Composable
fun JoinRoomDialog(
  tournament: CustomTournament,
  onDismiss: () -> Unit,
  onSubmit: (phone: String, uid: String, level: Int) -> Unit
) {
  var phone by remember { mutableStateOf("") }
  var uid by remember { mutableStateOf("") }
  var level by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { 
      Text(
        text = "Join ${tournament.name}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      ) 
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Please fill out candidate information to apply.", style = MaterialTheme.typography.bodySmall)
        
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone No.") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("join_room_phone")
        )
        OutlinedTextField(
          value = uid,
          onValueChange = { uid = it },
          label = { Text("In-Game UID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("join_room_uid")
        )
        OutlinedTextField(
          value = level,
          onValueChange = { level = it },
          label = { Text("Player Level (Min 51)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("join_room_level")
        )
        
        if (error != null) {
          Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val lvl = level.toIntOrNull() ?: 0
          if (lvl < 51) {
            error = "Level must be 51 or above to participate."
          } else if (phone.isBlank() || uid.isBlank()) {
            error = "All fields are required."
          } else {
            error = null
            onSubmit(phone, uid, lvl)
          }
        },
        modifier = Modifier.testTag("join_room_submit")
      ) {
        Text("Submit")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}


@Composable
fun ApplyCandidateDialog(
  profile: CustomProfile,
  defaultName: String = "",
  defaultUid: String = "",
  defaultPhone: String = "",
  onDismiss: () -> Unit,
  onSubmit: (name: String, phone: String, uid: String, level: String, rank: String) -> Unit
) {
  val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val safeDismiss = {
    keyboardController?.hide()
    focusManager.clearFocus()
    onDismiss()
  }

  var name by remember(defaultName) { mutableStateOf(defaultName) }
  var uid by remember(defaultUid) { mutableStateOf(defaultUid) }
  var level by remember { mutableStateOf("55") }
  var ruleAccepted by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  val isLocked = profile.candidateCount >= 7

  AlertDialog(
    onDismissRequest = safeDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    title = { 
      Text(
        text = "Apply Candidate",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      ) 
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (isLocked) {
          androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Candidate limit (7/7) has been reached. Registrations are closed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
        
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name -") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("apply_candidate_name")
        )
        
        val lvl = level.toIntOrNull() ?: 0
        val isLevelError = level.isNotBlank() && lvl < 51
        OutlinedTextField(
          value = level,
          onValueChange = { level = it },
          label = { Text("Lvl - (minimum 51 will accept)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          isError = isLevelError,
          modifier = Modifier.fillMaxWidth().testTag("apply_candidate_level")
        )
        
        OutlinedTextField(
          value = uid,
          onValueChange = { uid = it },
          label = { Text("UID -") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("apply_candidate_uid")
        )
        Text(
            text = "Rule info:\nPlayer must use the same UID that was submitted in the registration form. If a different UID is used during the match the player will be disqualified.",
            style = MaterialTheme.typography.bodySmall,
            color = if (ruleAccepted) SuccessGreen else MaterialTheme.colorScheme.error
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Checkbox(
                checked = ruleAccepted,
                onCheckedChange = { ruleAccepted = it }
            )
            Text("I agree to the rules", style = MaterialTheme.typography.bodyMedium)
        }
        
        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
      }
    },
    confirmButton = {
      val lvl = level.toIntOrNull() ?: 0
      val isFormValid = name.isNotBlank() && uid.isNotBlank() && level.isNotBlank() && lvl >= 51
      
      Button(
        onClick = {
          if (name.isBlank() || uid.isBlank() || level.isBlank()) {
              error = "All fields are required"
          } else if (lvl < 51) {
              error = "Level must be 51 or above"
          } else {
              error = null
              keyboardController?.hide()
              focusManager.clearFocus()
              onSubmit(name, defaultPhone, uid, level, "")
          }
        },
        enabled = ruleAccepted && isFormValid && !isLocked,
        modifier = Modifier.testTag("submit_application_button")
      ) {
        Text(if (isLocked) "Registration Locked" else "Submit")
      }
    },
    dismissButton = {
      TextButton(onClick = safeDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun CustomProfileCard(
  profile: CustomProfile,
  onJoinClick: () -> Unit,
  onSubmit: (() -> Unit)? = null,
  onEdit: (() -> Unit)? = null,
  onDelete: (() -> Unit)? = null,
  onViewCandidatesClick: (() -> Unit)? = null,
  onSendCredentialsClick: (() -> Unit)? = null,
  isJoined: Boolean = false,
  isOwnRoom: Boolean = false,
  isHostUser: Boolean = false,
  isAdmin: Boolean = false,
  candidateCountBadge: Int = 0,
  acceptedCandidateName: String? = null,
  hasSubmittedResult: Boolean = false,
  hasSubmittedReport: Boolean = false,
  onAdminViewCandidates: (() -> Unit)? = null,
  onAdminViewResultsAndReports: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val dayDisplay = formatMatchDisplayDate(profile.day).ifBlank { profile.day }
  
  val timeDisplay = profile.time.ifBlank { "TBA" }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
      val context = androidx.compose.ui.platform.LocalContext.current

      // Top Row: Avatar, Name, Day, Time, Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        coil.compose.AsyncImage(
          model = profile.imageUrl,
          contentDescription = "Avatar",
          contentScale = androidx.compose.ui.layout.ContentScale.Crop,
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = profile.name.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )

          if (isAdmin) {
              val hostDisplayName = profile.hostActualName.ifBlank { profile.name }
              val hostPhoneOrEmail = profile.hostPhone.ifBlank { profile.hostEmail }
              if (hostDisplayName.isNotBlank() || hostPhoneOrEmail.isNotBlank()) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                      Text(
                          text = "Host: $hostDisplayName",
                          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.primary,
                          maxLines = 1,
                          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                      )
                      if (hostPhoneOrEmail.isNotBlank()) {
                          Text(
                              text = "• $hostPhoneOrEmail",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                          )
                      }
                  }
              }
          } else if (isOwnRoom && isHostUser) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                  Text(
                      text = "Host: You",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
              }
          }

          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
              val aestheticColor = SuccessGreen
              androidx.compose.material3.Surface(
                  color = aestheticColor.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, aestheticColor.copy(alpha = 0.3f))
              ) {
                  Text(
                      text = "Day - $dayDisplay", 
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), 
                      color = aestheticColor,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
              }
              Spacer(modifier = Modifier.width(8.dp))
              androidx.compose.material3.Surface(
                  color = aestheticColor.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, aestheticColor.copy(alpha = 0.3f))
              ) {
                  Text(
                      text = "Time - $timeDisplay", 
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), 
                      color = aestheticColor,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
              }
          }
        }
        
        if (isOwnRoom && !isAdmin) {
          Row {
            if (onEdit != null) {
              IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
              }
            }
            if (onDelete != null) {
              IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
              }
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Middle Row: UID & Lv, Gun
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "UID - ${profile.uid}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                  Spacer(modifier = Modifier.width(4.dp))
                  IconButton(
                      onClick = {
                          clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(profile.uid))
                          android.widget.Toast.makeText(context, "UID copied", android.widget.Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(24.dp)
                  ) {
                      Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(16.dp))
                  }
              }
              Text(text = "L.v. - ${profile.level}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
          }
          
          if (profile.category == "BR" && profile.perKill.isNotBlank() && profile.perKill != "0") {
              Text(text = "Per Kill - ₹${profile.perKill}", style = MaterialTheme.typography.bodyMedium, color = TrophyGold, fontWeight = FontWeight.Bold)
          } else if (profile.category == "Custom" || (profile.category == "BR" && profile.gun != "All" && profile.gun.isNotBlank())) {
              val displayText = if (profile.gun == "Lone Wolf") "Game - Lone Wolf" else "Gun - ${profile.gun}"
              Text(text = displayText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
          }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Info Row: Payout | Type | Mode
      Row(
         modifier = Modifier
             .fillMaxWidth()
             .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
             .padding(vertical = 12.dp),
         horizontalArrangement = Arrangement.SpaceEvenly,
         verticalAlignment = Alignment.CenterVertically
      ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text(if (profile.category == "BR") "Entry Fee" else "Payout", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(4.dp))
              androidx.compose.material3.Surface(
                  color = SuccessGreen.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
              ) {
                  Text(
                      text = "₹${profile.payout}",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                      color = SuccessGreen,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
              }
          }
          androidx.compose.material3.VerticalDivider(
              modifier = Modifier.height(32.dp),
              color = MaterialTheme.colorScheme.outlineVariant
          )
          if (profile.category == "BR") {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                  Text("Prize Pool", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  androidx.compose.material3.Surface(
                      color = TrophyGold.copy(alpha = 0.15f),
                      shape = RoundedCornerShape(4.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, TrophyGold.copy(alpha = 0.3f))
                  ) {
                      Text(
                          text = "₹${profile.prizePool}",
                          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                          color = TrophyGold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                  }
              }
              androidx.compose.material3.VerticalDivider(
                  modifier = Modifier.height(32.dp),
                  color = MaterialTheme.colorScheme.outlineVariant
              )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = profile.type,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
              )
          }
          androidx.compose.material3.VerticalDivider(
              modifier = Modifier.height(32.dp),
              color = MaterialTheme.colorScheme.outlineVariant
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
              Text("Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = profile.mode,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
              )
          }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      val effectiveCandidates = maxOf(profile.candidateCount, candidateCountBadge)
      val totalPlayersCount = profile.totalPlayers.toIntOrNull() ?: 0
      val isCandidateLimitReached = effectiveCandidates >= 7
      val isRoomLocked = isCandidateLimitReached
      val isRoomFull = if (profile.category == "BR") {
          (totalPlayersCount > 0 && profile.joinedPlayers >= totalPlayersCount) || isRoomLocked
      } else {
          isRoomLocked
      }
      val buttonEnabled = !isJoined && !isRoomFull && !isRoomLocked

      // Candidates Count and Lockout Status Row
      Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  imageVector = Icons.Default.Groups,
                  contentDescription = null,
                  tint = if (isCandidateLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  text = "Candidates: ${effectiveCandidates}/7",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = if (isCandidateLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
              )
          }
          if (isCandidateLimitReached) {
              androidx.compose.material3.Surface(
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
              ) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                      Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                          text = "FULL (7/7)",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.error
                      )
                  }
              }
          }
      }

      if (profile.category == "BR" && profile.totalPlayers.isNotBlank() && profile.totalPlayers != "0") {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.Default.Groups,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                      text = "${profile.joinedPlayers}/$totalPlayersCount Joined",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
              }
              
              val progress = if (totalPlayersCount > 0) profile.joinedPlayers.toFloat() / totalPlayersCount.toFloat() else 0f
              androidx.compose.material3.LinearProgressIndicator(
                  progress = { progress.coerceIn(0f, 1f) },
                  modifier = Modifier.weight(1f).padding(start = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                  color = SuccessGreen,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
              )
          }
          Spacer(modifier = Modifier.height(12.dp))
      }

      if (isAdmin) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              // 1. View Candidates Button with eye icon
              Button(
                  onClick = { onAdminViewCandidates?.invoke() },
                  modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .testTag("admin_view_candidates_${profile.id}"),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(
                      containerColor = if (acceptedCandidateName != null) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                      contentColor = if (acceptedCandidateName != null) SuccessGreen else MaterialTheme.colorScheme.onSurface
                  ),
                  border = BorderStroke(
                      1.dp,
                      if (acceptedCandidateName != null) SuccessGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                  ),
                  contentPadding = PaddingValues(horizontal = 8.dp)
              ) {
                  Icon(
                      imageVector = Icons.Default.Visibility,
                      contentDescription = "View Candidates",
                      modifier = Modifier.size(18.dp),
                      tint = if (acceptedCandidateName != null) SuccessGreen else MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "Candidates ($candidateCountBadge)",
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          maxLines = 1
                      )
                      Text(
                          text = if (acceptedCandidateName != null) "✓ Acc: $acceptedCandidateName" else "Pending Host",
                          style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 10.sp,
                              fontWeight = if (acceptedCandidateName != null) FontWeight.Bold else FontWeight.Normal,
                              color = if (acceptedCandidateName != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                          ),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                      )
                  }
              }

              // 2. Results & Reports Button with score/flag icon
              Button(
                  onClick = { onAdminViewResultsAndReports?.invoke() },
                  modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .testTag("admin_results_reports_${profile.id}"),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(
                      containerColor = when {
                          hasSubmittedReport -> AlertRed.copy(alpha = 0.18f)
                          hasSubmittedResult -> TrophyGold.copy(alpha = 0.18f)
                          else -> MaterialTheme.colorScheme.surfaceVariant
                      },
                      contentColor = when {
                          hasSubmittedReport -> AlertRed
                          hasSubmittedResult -> TrophyGold
                          else -> MaterialTheme.colorScheme.onSurface
                      }
                  ),
                  border = BorderStroke(
                      1.dp,
                      when {
                          hasSubmittedReport -> AlertRed.copy(alpha = 0.7f)
                          hasSubmittedResult -> TrophyGold.copy(alpha = 0.7f)
                          else -> MaterialTheme.colorScheme.outlineVariant
                      }
                  ),
                  contentPadding = PaddingValues(horizontal = 8.dp)
              ) {
                  val resultIcon = when {
                      hasSubmittedReport -> Icons.Default.Flag
                      hasSubmittedResult -> Icons.Default.EmojiEvents
                      else -> Icons.Default.Assessment
                  }
                  Icon(
                      imageVector = resultIcon,
                      contentDescription = "Match Result & Reports",
                      modifier = Modifier.size(18.dp),
                      tint = when {
                          hasSubmittedReport -> AlertRed
                          hasSubmittedResult -> TrophyGold
                          else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = "Match Results",
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          maxLines = 1
                      )
                      val statusText = when {
                          hasSubmittedReport -> "Dispute Report!"
                          hasSubmittedResult -> "Results Ready"
                          else -> "Host & Candidate"
                      }
                      Text(
                          text = statusText,
                          style = MaterialTheme.typography.labelSmall.copy(
                              fontSize = 10.sp,
                              fontWeight = if (hasSubmittedReport || hasSubmittedResult) FontWeight.Bold else FontWeight.Normal,
                              color = when {
                                  hasSubmittedReport -> AlertRed
                                  hasSubmittedResult -> TrophyGold
                                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                              }
                          ),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                      )
                  }
              }
          }
      } else if (isOwnRoom) {
          Row(
              modifier = Modifier.fillMaxWidth().height(48.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
              if (onViewCandidatesClick != null) {
                  androidx.compose.material3.OutlinedButton(
                      onClick = onViewCandidatesClick,
                      modifier = Modifier.weight(1f).fillMaxHeight(),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(0.dp)
                  ) {
                      Icon(Icons.Default.Visibility, contentDescription = "View", modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("View", style = MaterialTheme.typography.labelMedium)
                  }
              }
              if (onSendCredentialsClick != null) {
                  Button(
                      onClick = onSendCredentialsClick,
                      modifier = Modifier.weight(1f).fillMaxHeight(),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(0.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                  ) {
                      Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Send", style = MaterialTheme.typography.labelMedium)
                  }
              }
              if (onSubmit != null) {
                  androidx.compose.material3.OutlinedButton(
                      onClick = onSubmit,
                      modifier = Modifier.weight(1f).fillMaxHeight(),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(0.dp)
                  ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = "Submit", modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Submit", style = MaterialTheme.typography.labelMedium)
                  }
              }
          }
      } else {
          val isHostViewingInBrowse = isHostUser && !isOwnRoom
          val effectiveEnabled = if (isHostViewingInBrowse || isJoined) true else buttonEnabled
          val effectiveContainerColor = when {
              isHostViewingInBrowse -> MaterialTheme.colorScheme.surfaceVariant
              isJoined -> SuccessGreen
              else -> SuccessGreen
          }
          val effectiveTextColor = when {
              isHostViewingInBrowse -> MaterialTheme.colorScheme.onSurface
              isJoined -> Color.White
              buttonEnabled -> Color.White
              isRoomLocked -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurfaceVariant
          }
          Button(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("join_match_${profile.id}"),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = effectiveContainerColor,
                contentColor = effectiveTextColor,
                disabledContainerColor = if (isRoomLocked) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = if (isRoomLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            enabled = effectiveEnabled
          ) {
            val buttonText = when {
                isHostViewingInBrowse -> "MY ROOM"
                isJoined -> "JOINED ✓"
                isCandidateLimitReached -> "ROOM FULL (7/7)"
                isRoomFull -> "ROOM FULL"
                else -> "JOIN MATCH"
            }
            if (isRoomLocked && !isJoined && !isHostViewingInBrowse) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(6.dp))
            } else if (isJoined) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(buttonText, fontWeight = FontWeight.Bold, color = effectiveTextColor, style = MaterialTheme.typography.labelLarge)
          }
      }
    }
  }
}

@Composable
fun MatchDetailsDialog(
  profile: CustomProfile,
  isAdmin: Boolean = false,
  userApplication: CustomProfileApplication? = null,
  onDismiss: () -> Unit,
  onProceed: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${profile.game} Match",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        // Scrollable Content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Tags
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallTag(icon = Icons.Default.SportsEsports, text = profile.game)
            SmallTag(icon = Icons.Default.Public, text = profile.mode)
            SmallTag(icon = Icons.Default.Groups, text = profile.type)
          }

          // Match Information
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
               MatchDetailRow("Category", profile.category)
               MatchDetailRow("Type", profile.type)
               MatchDetailRow("Mode", profile.mode)
               MatchDetailRow(if (profile.gun == "Lone Wolf") "Game" else "Gun Rules", profile.gun.ifBlank { "Any" })
               MatchDetailRow("Level Required", "Min ${profile.level}")
               MatchDetailRow("Schedule", "${formatMatchDisplayDate(profile.day).ifBlank { profile.day }} at ${profile.time}")
            }
          }

          // Custom Room Access Card
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("custom_room_access_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (userApplication != null && !userApplication.roomId.isNullOrBlank()) Icons.Default.Campaign else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (userApplication != null && !userApplication.roomId.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Custom Room Access",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                if (userApplication != null && !userApplication.roomId.isNullOrBlank()) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "BROADCASTED ✓",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = SuccessGreen,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              if (userApplication != null && !userApplication.roomId.isNullOrBlank() && !userApplication.roomPassword.isNullOrBlank()) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                      text = "Host Broadcast Message: Room credentials are live!",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                          Text("Room ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                          Text(userApplication.roomId ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                      }
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                      ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                          Text("Password", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                          Text(userApplication.roomPassword ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                      }
                    }
                  }
                }
              } else if (userApplication != null) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.AccessTime,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Registered! Awaiting host broadcast: Room ID & Password will be delivered here once host sends credentials.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              } else {
                Text(
                  text = "Room ID and Password will be provided by the host after joining.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Prize Distribution
          if (profile.category == "BR" && profile.prizePool.isNotBlank()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TrophyGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prize Distribution", fontWeight = FontWeight.Bold)
                  }
                  Text("Pool: ₹${profile.prizePool}", fontWeight = FontWeight.Bold, color = TrophyGold)
                }
                if (profile.perKill.isNotBlank() && profile.perKill != "0") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Per Kill", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${profile.perKill}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  "Total prize pool is distributed according to the host's tournament rules based on rank and kills.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Tournament Rules & Policy
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Tournament Rules & Policy",
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "1. Emulators strictly banned.\n2. Hacks/mods will result in a permanent ban.\n3. Ensure your in-game name matches your profile name exactly.\n4. Join the room 10 minutes prior to the start time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
              )
            }
          }
          
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Sticky Bar
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Entry Fee",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = if (profile.payout == "0" || profile.payout.isBlank()) "Free" else "₹${profile.payout}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            if (isAdmin) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "ADMINISTRATOR VIEW",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            } else {
              val isLocked = profile.candidateCount >= 7
              val isJoined = userApplication != null
              val buttonText = when {
                  isJoined -> "JOINED ✓"
                  profile.candidateCount >= 7 -> "ROOM FULL (7/7)"
                  else -> "JOIN MATCH"
              }
              Button(
                onClick = onProceed,
                enabled = !isLocked && !isJoined,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen,
                    disabledContainerColor = if (isJoined) SuccessGreen else if (isLocked) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = if (isJoined) Color.White else if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
              ) {
                Icon(
                  if (isJoined) Icons.Default.CheckCircle else if (isLocked) Icons.Default.Lock else Icons.Default.SportsEsports,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SmallTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(
    modifier = Modifier
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.width(4.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun InfoCard(
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  value1: String,
  value2: String
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(value1, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(2.dp))
      Text(value2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun MatchDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
            color = MaterialTheme.colorScheme.onSurface, 
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun SubmitSummaryDialog(
    profile: CustomProfile,
    onDismiss: () -> Unit
) {
    val entryFee = profile.payout.toIntOrNull() ?: 0
    val totalFund = profile.joinedPlayers * entryFee
    val prizePool = profile.prizePool.toIntOrNull() ?: 0
    val perKillStr = profile.perKill.trim()
    val hasPerKill = perKillStr.isNotEmpty() && perKillStr != "0" && perKillStr.toIntOrNull() != 0
    
    val perKillAllocation = if (hasPerKill) (totalFund * 0.20).toInt() else 0
    val profit = totalFund - prizePool - perKillAllocation

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Match Financial Summary", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Room: ${profile.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                MatchDetailRow("Entry Fee", "₹$entryFee")
                MatchDetailRow("Joined Players", "${profile.joinedPlayers} / ${profile.totalPlayers}")
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Collected Fund", fontWeight = FontWeight.Bold)
                    Text("₹$totalFund", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prize Pool Set", color = AlertRed)
                    Text("- ₹$prizePool", color = AlertRed, fontWeight = FontWeight.SemiBold)
                }
                
                if (hasPerKill) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Per Kill (20% of Fund)", color = AlertRed)
                        Text("- ₹$perKillAllocation", color = AlertRed, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "Paid according to player kills",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                val profitColor = if (profit >= 0) SuccessGreen else AlertRed
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Profit", fontWeight = FontWeight.Bold, color = profitColor)
                    Text("₹$profit", fontWeight = FontWeight.Bold, color = profitColor)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
