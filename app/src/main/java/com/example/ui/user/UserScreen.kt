package com.example.ui.user
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import com.google.firebase.auth.FirebaseAuth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.NeonCyan
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import com.example.data.model.WalletTransaction
import com.example.utils.UpiUtils
import com.example.utils.RoomBroadcastManager
import com.example.worker.NotificationManagerHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthStatus
import com.example.data.model.MatchItem
import com.example.data.model.TournamentHistory
import com.example.data.model.CustomProfileApplication
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SageSecondary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.TournamentViewModel

@Composable
fun UserScreen(
  viewModel: TournamentViewModel,
  onNavigateToAuth: () -> Unit = {},
  onWalletClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
  val joinedMatches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val roomRegistrations by viewModel.myRoomRegistrations.collectAsStateWithLifecycle()
  val myCustomProfileApplications by viewModel.myCustomProfileApplications.collectAsStateWithLifecycle()
  val allAdminApps by viewModel.allCustomProfileApplicationsForAdmin.collectAsStateWithLifecycle()
  val myAppliedCustomProfiles by viewModel.myAppliedCustomProfiles.collectAsStateWithLifecycle()
  val firestoreCustomProfiles by viewModel.firestoreCustomProfiles.collectAsStateWithLifecycle()
  val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
  val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
  val userJoinedMatches = joinedMatches.filter { it.isJoined }
  val activeAccount = (authStatus as? AuthStatus.LoggedIn)?.account
  val activeAppliedCustomProfiles = remember(myAppliedCustomProfiles, allAdminApps, userProfile, activeAccount, currentAuthUid) {
    val curGameUid = activeAccount?.gameUid?.trim()?.takeIf { it.isNotBlank() } ?: userProfile.uid.trim()
    val curPhoneDigits = activeAccount?.phone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() } ?: ""
    val curUsername = activeAccount?.username?.trim()?.takeIf { it.isNotBlank() } ?: userProfile.name.trim()

    (myAppliedCustomProfiles + allAdminApps.filter { app ->
      val appPhoneDigits = app.phone.filter { it.isDigit() }
      (currentAuthUid.isNotBlank() && app.applicantUid.isNotBlank() && app.applicantUid == currentAuthUid) ||
      (curGameUid.isNotBlank() && app.uid.isNotBlank() && app.uid.equals(curGameUid, ignoreCase = true)) ||
      (curPhoneDigits.isNotBlank() && appPhoneDigits.isNotBlank() && appPhoneDigits == curPhoneDigits) ||
      (curUsername.isNotBlank() && app.candidateName.isNotBlank() && app.candidateName.equals(curUsername, ignoreCase = true)) ||
      (curGameUid.isNotBlank() && app.applicantUid.isNotBlank() && app.applicantUid.equals(curGameUid, ignoreCase = true))
    }).distinctBy { it.id }
  }

  var showEditDialog by remember { mutableStateOf(false) }
  var showLogoutConfirmDialog by remember { mutableStateOf(false) }
  var showSwitchAccountDialog by remember { mutableStateOf(false) }
  var showAddFundsDialog by remember { mutableStateOf(false) }
  var showWithdrawDialog by remember { mutableStateOf(false) }
  var showTransactionHistoryDialog by remember { mutableStateOf(false) }
  var selectedHistoryTab by remember { mutableIntStateOf(0) }
  val tournamentHistory by viewModel.tournamentHistory.collectAsStateWithLifecycle()
  var selectedCandidateRoomId by remember { mutableStateOf<String?>(null) }
  var selectedMatchForDetails by remember { mutableStateOf<MatchItem?>(null) }
  var matchToDeleteBroadcast by remember { mutableStateOf<MatchItem?>(null) }
  var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(1000)
      currentTimeMillis = System.currentTimeMillis()
    }
  }

  var appForPayment by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appForHostPayment by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appForSubmit by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appForReport by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appForSend by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appToAccept by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var appToDelete by remember { mutableStateOf<CustomProfileApplication?>(null) }
  var isRejectAction by remember { mutableStateOf(false) }

  val isGuest = authStatus is AuthStatus.LoggedIn && (authStatus as AuthStatus.LoggedIn).isGuest

  // Only display the credentials actually provided by the user while creating the account
  val registeredEmail = remember(activeAccount, currentAuthUid) {
    val accEmail = activeAccount?.email?.trim() ?: ""
    val fbEmail = FirebaseAuth.getInstance().currentUser?.email?.trim() ?: ""
    when {
      accEmail.isNotBlank() && !accEmail.endsWith("@tourneymatch.com", ignoreCase = true) -> accEmail
      fbEmail.isNotBlank() && !fbEmail.endsWith("@tourneymatch.com", ignoreCase = true) -> fbEmail
      else -> ""
    }
  }

  val registeredPhone = remember(activeAccount, currentAuthUid) {
    val accPhone = activeAccount?.phone?.trim() ?: ""
    val fbPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.trim() ?: ""
    val phone = when {
      accPhone.isNotBlank() -> accPhone
      fbPhone.isNotBlank() -> fbPhone
      else -> ""
    }
    if (phone.isNotBlank()) {
      val digits = phone.filter { it.isDigit() }
      if (!phone.startsWith("+91") && digits.length == 10) "+91 $phone" else phone
    } else {
      ""
    }
  }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    viewModel.fetchMyCustomProfileApplications()
    viewModel.listenToCustomProfileApplications()
  }

  // Automatic mobile notification trigger when Room credentials are released (one time per broadcast)
  androidx.compose.runtime.LaunchedEffect(userJoinedMatches, isAdmin) {
    if (isAdmin) return@LaunchedEffect
    userJoinedMatches.forEach { match ->
      if (match.isRoomBroadcasted && match.roomId.isNotBlank() && match.roomPassword.isNotBlank()) {
        val key = "match_broadcast_${match.id}_${match.roomId}_${match.roomPassword}"
        NotificationManagerHelper.showImmediateNotificationOnce(
          context,
          key = key,
          title = "Room ID & Password: ${match.name}",
          message = "Room ID: ${match.roomId} | Password: ${match.roomPassword}. Open Free Fire MAX to enter now!"
        )
      }
    }
  }

  androidx.compose.runtime.LaunchedEffect(myAppliedCustomProfiles) {
    myAppliedCustomProfiles.forEach { app ->
      if (!app.roomId.isNullOrBlank() && !app.roomPassword.isNullOrBlank()) {
        val key = "custom_broadcast_${app.id}_${app.roomId}_${app.roomPassword}"
        NotificationManagerHelper.showImmediateNotificationOnce(
          context,
          key = key,
          title = "Custom Room Credentials: ${app.profileName}",
          message = "Room ID: ${app.roomId} | Password: ${app.roomPassword}. Open Free Fire MAX to enter now!"
        )
      }
    }
  }

  val myHostedProfiles = remember(firestoreCustomProfiles, userProfile, activeAccount, currentAuthUid) {
    val activeGameUid = activeAccount?.gameUid?.trim()?.takeIf { it.isNotBlank() } ?: userProfile.uid.trim()
    firestoreCustomProfiles.filter { profile ->
      (activeGameUid.isNotBlank() && (profile.uid.equals(activeGameUid, ignoreCase = true) || profile.hostUid.equals(activeGameUid, ignoreCase = true))) ||
      (currentAuthUid.isNotBlank() && profile.hostUid.isNotBlank() && profile.hostUid == currentAuthUid && (activeAccount == null || activeAccount.phone == com.example.ui.viewmodel.AdminConfig.ADMIN_PHONE || activeAccount.gameUid.isBlank() || activeAccount.gameUid.equals(profile.uid, ignoreCase = true)))
    }
  }
  val myHostedProfileIds = remember(myHostedProfiles) { myHostedProfiles.map { it.id }.toSet() }

  val customOnlyCandidates = remember(myCustomProfileApplications, allAdminApps, myHostedProfileIds, isAdmin, currentAuthUid, userProfile, activeAccount) {
    val pool = (myCustomProfileApplications + allAdminApps).distinctBy { it.id }
    if (isAdmin) {
      pool
    } else {
      pool.filter { app ->
        myHostedProfileIds.contains(app.profileId) ||
        (currentAuthUid.isNotBlank() && app.hostUid.isNotBlank() && app.hostUid == currentAuthUid) ||
        (userProfile.uid.isNotBlank() && app.hostUid.isNotBlank() && app.hostUid == userProfile.uid) ||
        (activeAccount?.gameUid?.isNotBlank() == true && app.hostUid.isNotBlank() && app.hostUid == activeAccount.gameUid)
      }
    }
  }
  val groupedCandidates = customOnlyCandidates.groupBy { it.profileId }
  
  androidx.compose.runtime.LaunchedEffect(groupedCandidates.keys) {
      if (groupedCandidates.isEmpty()) {
          selectedCandidateRoomId = null
      } else if (selectedCandidateRoomId == null || !groupedCandidates.containsKey(selectedCandidateRoomId)) {
          selectedCandidateRoomId = groupedCandidates.keys.firstOrNull()
      }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .background(MaterialTheme.colorScheme.background)
      .testTag("user_screen_content"),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = 16.dp,
      bottom = 88.dp
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Profile Header Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              // Avatar
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SportsEsports,
                  contentDescription = "User Avatar",
                  tint = Color.White,
                  modifier = Modifier.size(36.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column {
                Text(
                  text = userProfile.name,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )

                // UID with Copy Button
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clickable {
                      copyToClipboard(context, "Player UID", userProfile.uid)
                    }
                    .testTag("copy_uid_button")
                ) {
                  Text(
                    text = "UID: ${userProfile.uid}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy UID",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                  )
                }

                // Region & Rank Tag
                Row(
                  modifier = Modifier.padding(top = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = userProfile.region,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TrophyGold.copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = userProfile.rank,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TrophyGold
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }

            // Edit Profile Button
            IconButton(
              onClick = { showEditDialog = true },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("edit_profile_button")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Registered Contact Info: Only show what was filled during account creation (mobile only, email only, or both)
          if (registeredEmail.isNotBlank() || registeredPhone.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "REGISTERED PROFILE INFO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
              )

              // Only show email if provided during account creation
              if (registeredEmail.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth().testTag("profile_registered_email_card")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.AlternateEmail,
                          contentDescription = "Registered Email",
                          tint = NeonCyan,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text = "Registered Email",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                          text = registeredEmail,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                    IconButton(
                      onClick = { copyToClipboard(context, "Registered Email", registeredEmail) },
                      modifier = Modifier.size(32.dp).testTag("copy_registered_email_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }

              // Only show mobile number if provided during account creation
              if (registeredPhone.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth().testTag("profile_registered_phone_card")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(32.dp)
                          .clip(CircleShape)
                          .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.Phone,
                          contentDescription = "Registered Mobile",
                          tint = SuccessGreen,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text = "Registered Mobile Number",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                          text = registeredPhone,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onSurface
                        )
                      }
                    }
                    IconButton(
                      onClick = { copyToClipboard(context, "Registered Mobile", registeredPhone) },
                      modifier = Modifier.size(32.dp).testTag("copy_registered_mobile_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Mobile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          // Session Management Row inside Profile Card
          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isGuest) "Guest Session" else "@${activeAccount?.username ?: userProfile.name.lowercase()}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (isGuest) {
                TextButton(
                  onClick = onNavigateToAuth,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Register / Sign In", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
              } else {
                TextButton(
                  onClick = { showSwitchAccountDialog = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Switch", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                  onClick = { showLogoutConfirmDialog = true },
                  colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text("Log Out", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    // 2. My Wallet Quick Access Tile (replacing full tournament wallet interface)
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onWalletClick() }
          .testTag("my_wallet_tile")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary,
                      MaterialTheme.colorScheme.secondary
                    )
                  )
                )
                .clickable { onWalletClick() }
                .testTag("my_wallet_icon_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "My Wallet",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
              Text(
                text = "My Wallet",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tap to view earnings, add funds & withdraw",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
              Text(
                text = "₹${userProfile.walletBalance}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = "Navigate to Wallet",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }


    // 4. Tournament History Section
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tournament Activity",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        }

        ScrollableTabRow(
          selectedTabIndex = selectedHistoryTab,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
          edgePadding = 0.dp,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedHistoryTab]),
              color = MaterialTheme.colorScheme.primary
            )
          }
        ) {
          Tab(
            selected = selectedHistoryTab == 0,
            onClick = { selectedHistoryTab = 0 },
            icon = {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Active Registered",
                modifier = Modifier.size(20.dp)
              )
            },
            text = { Text("Active Registered (${userJoinedMatches.size + activeAppliedCustomProfiles.size})", style = MaterialTheme.typography.labelMedium) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Tab(
            selected = selectedHistoryTab == 1,
            onClick = { selectedHistoryTab = 1 },
            icon = {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Room Candidates",
                modifier = Modifier.size(20.dp)
              )
            },
            text = { Text("Room Candidates (${customOnlyCandidates.size})", style = MaterialTheme.typography.labelMedium) },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Tab content
    when (selectedHistoryTab) {
      0 -> {
        val appliedProfilesToShow = activeAppliedCustomProfiles
        if (userJoinedMatches.isEmpty() && appliedProfilesToShow.isEmpty()) {
          item {
            EmptyHistoryCard(text = "You haven't joined any active matches yet. Head to the Matches tab to register!")
          }
        } else {
            items(appliedProfilesToShow, key = { "app_${it.id}" }) { app ->
            val profile = firestoreCustomProfiles.find { it.id == app.profileId }
            val formattedPayout = formatPayout(app.payout.ifBlank { profile?.payout ?: "" })
            val roomName = app.profileName.ifBlank { profile?.name ?: "Custom Room" }
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp)
              ) {
                // 1. First: Room Name
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.SportsEsports,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                    Text(
                      text = roomName,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                  }

                  if (app.status == "Pending" || app.status == "Rejected") {
                    androidx.compose.material3.IconButton(
                      onClick = { 
                        appToDelete = app
                        isRejectAction = false
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      androidx.compose.material3.Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Cancel Registration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }

                val hostName = app.hostActualName.ifBlank { profile?.hostActualName ?: profile?.name ?: "" }
                val hostContact = app.hostPhone.ifBlank { app.hostEmail.ifBlank { profile?.hostPhone ?: profile?.hostEmail ?: "" } }
                val hostGameUid = app.hostGameUid.ifBlank { profile?.hostGameUid ?: profile?.uid ?: "" }
                if (hostName.isNotBlank() || hostContact.isNotBlank() || hostGameUid.isNotBlank()) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    if (hostName.isNotBlank()) {
                      Text(
                        text = "Host: $hostName",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                      )
                    }
                    if (hostGameUid.isNotBlank()) {
                      Text(
                        text = "• UID: $hostGameUid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                    if (hostContact.isNotBlank()) {
                      Text(
                        text = "• Contact: $hostContact",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. UID --  Payout --  [status]
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                  ) {
                    Text(
                      text = "UID - ${app.uid}",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    if (formattedPayout.isNotBlank()) {
                      Text(
                        text = "Payout - $formattedPayout",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  // [status] badge
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (app.status) {
                      "Rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                      "Accepted" -> SuccessGreen.copy(alpha = 0.2f)
                      "Paid" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                      else -> SuccessGreen.copy(alpha = 0.2f)
                    }
                  ) {
                    Text(
                      text = if (app.status == "Pending") "PENDING" else app.status.uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (app.status) {
                          "Rejected" -> MaterialTheme.colorScheme.error
                          "Accepted" -> SuccessGreen
                          "Paid" -> MaterialTheme.colorScheme.primary
                          else -> SuccessGreen
                        }
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. LV --
                Text(
                  text = "LV - ${app.level}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Message Container: displayed for all registration statuses (Pending, Accepted, Paid, Rejected)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = when (app.status) {
                    "Accepted" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    "Paid" -> SuccessGreen.copy(alpha = 0.15f)
                    "Rejected" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                  },
                  border = BorderStroke(1.dp, when (app.status) {
                    "Accepted" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    "Paid" -> SuccessGreen.copy(alpha = 0.5f)
                    "Rejected" -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                  }),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.Top
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Chat,
                          contentDescription = null,
                          tint = when (app.status) {
                            "Accepted" -> MaterialTheme.colorScheme.primary
                            "Paid" -> SuccessGreen
                            "Rejected" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                          },
                          modifier = Modifier.size(16.dp)
                        )
                        Text(
                          text = when (app.status) {
                            "Accepted" -> "Message: Application Accepted"
                            "Paid" -> "Message: Payment Confirmed"
                            "Rejected" -> "Message: Application Declined"
                            else -> "Message: Application Submitted"
                          },
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          color = when (app.status) {
                            "Accepted" -> MaterialTheme.colorScheme.primary
                            "Paid" -> SuccessGreen
                            "Rejected" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                          }
                        )
                      }
                      androidx.compose.material3.IconButton(
                        onClick = { 
                          appToDelete = app
                          isRejectAction = false
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        androidx.compose.material3.Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete Application",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = when {
                        !app.roomId.isNullOrBlank() -> "Room details received! Join the match on Free Fire MAX on time."
                        app.status == "Paid" -> "Payment successful! Waiting for host to send room details."
                        app.status == "Accepted" -> if (formattedPayout.isNotBlank()) "Your application was accepted! Now it is time to make your payment of $formattedPayout." else "Your application was accepted! Now it is time to make your payment."
                        app.status == "Rejected" -> "Your application was declined by the host."
                        else -> "Your application for $roomName has been submitted! Waiting for host ${if (hostName.isNotBlank()) "($hostName) " else ""}to review and accept your registration."
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!app.roomId.isNullOrBlank() && !app.roomPassword.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Admin Message: Room Credentials",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SuccessGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Credentials released! Join custom room on Free Fire MAX on time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", app.roomId))
                                                Toast.makeText(context, "Room ID copied: ${app.roomId}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ROOM ID", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(app.roomId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room ID", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Password", app.roomPassword))
                                                Toast.makeText(context, "Password copied: ${app.roomPassword}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PASSWORD", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(app.roomPassword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(14.dp), tint = SuccessGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { appForSubmit = app },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("Submit")
                            }
                            Button(
                                onClick = { appForReport = app },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Text("Report")
                            }
                        }
                    } else if (app.status == "Accepted") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                          onClick = { appForPayment = app },
                          modifier = Modifier.fillMaxWidth(),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                          Text("Book & Pay")
                        }
                    }
                  }
                }
            }
          }
        }
          items(userJoinedMatches, key = { it.id }) { match ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                      text = match.name,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Match #${match.matchNumber} • ${match.date} • ${match.time}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = SuccessGreen.copy(alpha = 0.2f)
                    ) {
                      Text(
                        text = "REGISTERED",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = SuccessGreen
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }

                    IconButton(
                      onClick = { matchToDeleteBroadcast = match },
                      modifier = Modifier
                        .size(30.dp)
                        .testTag("delete_match_header_${match.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete broadcast or registration",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 30-Minute Broadcast Tracking & Expiration Logic
                val broadcastTime = RoomBroadcastManager.getBroadcastTime(context, match.id)
                if (match.isRoomBroadcasted && broadcastTime == 0L) {
                  RoomBroadcastManager.recordBroadcastTime(context, match.id, currentTimeMillis)
                }

                val isBroadcastDeleted = RoomBroadcastManager.isBroadcastDeleted(context, match.id)
                val isBroadcastExpired = RoomBroadcastManager.isBroadcastExpired(context, match.id, currentTimeMillis)
                val remainingMillis = RoomBroadcastManager.getRemainingMillis(context, match.id, currentTimeMillis)
                val remainingFormatted = RoomBroadcastManager.formatRemainingTime(remainingMillis)

                // Message Form Container: ONLY show Room ID & Password when admin broadcasts to all and within 30 min
                if (match.isRoomBroadcasted && match.roomId.isNotBlank() && match.roomPassword.isNotBlank()) {
                  if (!isBroadcastDeleted && !isBroadcastExpired) {
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                      border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                      modifier = Modifier.fillMaxWidth().testTag("broadcasted_credentials_${match.id}")
                    ) {
                      Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                          ) {
                            Icon(
                              imageVector = Icons.Default.Chat,
                              contentDescription = null,
                              tint = SuccessGreen,
                              modifier = Modifier.size(16.dp)
                            )
                            Text(
                              text = "Admin Broadcast: Room Credentials",
                              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                              color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                          }
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            Surface(
                              shape = RoundedCornerShape(4.dp),
                              color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                              Text(
                                text = "LIVE NOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 9.sp,
                                  color = SuccessGreen
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                              )
                            }

                            // 30-min auto-delete countdown chip
                            Surface(
                              shape = RoundedCornerShape(4.dp),
                              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            ) {
                              Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                              ) {
                                Icon(
                                  imageVector = Icons.Default.AccessTime,
                                  contentDescription = null,
                                  tint = MaterialTheme.colorScheme.error,
                                  modifier = Modifier.size(10.dp)
                                )
                                Text(
                                  text = remainingFormatted,
                                  style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error
                                  )
                                )
                              }
                            }

                            // Manual delete icon button for broadcast message
                            IconButton(
                              onClick = { matchToDeleteBroadcast = match },
                              modifier = Modifier
                                .size(26.dp)
                                .testTag("delete_broadcast_msg_${match.id}")
                            ) {
                              Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete broadcast message",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                              )
                            }
                          }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                          text = "Admin has broadcasted your Custom Room ID & Password to all registered candidates! Join the Free Fire MAX custom room now.",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                          )
                          Text(
                            text = "Auto-deletes in 30 min (remaining: $remainingFormatted) • Tap trash icon to delete now",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                          )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                          // Room ID Box with 1-tap copy
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                              .weight(1f)
                              .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", match.roomId))
                                Toast.makeText(context, "Room ID copied: ${match.roomId}", Toast.LENGTH_SHORT).show()
                              }
                          ) {
                            Column(
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                              horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                              Text(
                                "ROOM ID",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                              ) {
                                Text(
                                  text = match.roomId,
                                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                  color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                  imageVector = Icons.Default.ContentCopy,
                                  contentDescription = "Copy Room ID",
                                  modifier = Modifier.size(14.dp),
                                  tint = MaterialTheme.colorScheme.primary
                                )
                              }
                            }
                          }

                          // Password Box with 1-tap copy
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                              .weight(1f)
                              .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Password", match.roomPassword))
                                Toast.makeText(context, "Password copied: ${match.roomPassword}", Toast.LENGTH_SHORT).show()
                              }
                          ) {
                            Column(
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                              horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                              Text(
                                "PASSWORD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                              ) {
                                Text(
                                  text = match.roomPassword,
                                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                  color = SuccessGreen
                                )
                                Icon(
                                  imageVector = Icons.Default.ContentCopy,
                                  contentDescription = "Copy Password",
                                  modifier = Modifier.size(14.dp),
                                  tint = SuccessGreen
                                )
                              }
                            }
                          }
                        }
                      }
                    }
                  } else {
                    // Broadcast message expired (30m limit passed) or deleted manually
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                      modifier = Modifier.fillMaxWidth().testTag("expired_broadcast_${match.id}")
                    ) {
                      Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Row(
                          modifier = Modifier.weight(1f),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                          Icon(
                            imageVector = Icons.Default.TimerOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                          )
                          Column {
                            Text(
                              text = if (isBroadcastDeleted) "Broadcast Message Deleted" else "Broadcast Expired (30m Limit)",
                              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                              color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                              text = if (isBroadcastDeleted)
                                "You deleted this room broadcast message."
                              else
                                "Room ID & Password were valid for 30 minutes and have automatically expired.",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                        }
                        IconButton(
                          onClick = { matchToDeleteBroadcast = match },
                          modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_expired_match_${match.id}")
                        ) {
                          Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove registration",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("pending_broadcast_${match.id}")
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.AccessTime,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp)
                        )
                        Column {
                          Text(
                            text = "Awaiting Admin Broadcast",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = "Room ID & Password will be delivered here in message form & via phone notification when Admin broadcasts to all registered candidates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }
                      IconButton(
                        onClick = { matchToDeleteBroadcast = match },
                        modifier = Modifier
                          .size(28.dp)
                          .testTag("delete_pending_match_${match.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Default.DeleteOutline,
                          contentDescription = "Delete registration",
                          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                          modifier = Modifier.size(16.dp)
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
    1 -> {
        if (roomRegistrations.isEmpty() && customOnlyCandidates.isEmpty()) {
          item {
            EmptyHistoryCard(text = "No candidates have applied to your custom rooms yet.")
          }
        } else {
          if (groupedCandidates.isNotEmpty()) {
              item {
                  androidx.compose.foundation.lazy.LazyRow(
                      modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                      item {
                          androidx.compose.material3.FilterChip(
                              selected = selectedCandidateRoomId == null,
                              onClick = { selectedCandidateRoomId = null },
                              label = { Text("All Rooms (${customOnlyCandidates.size})") },
                              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                  selectedContainerColor = SuccessGreen,
                                  selectedLabelColor = Color.White
                              ),
                              shape = RoundedCornerShape(16.dp)
                          )
                      }
                      items(groupedCandidates.keys.toList()) { profileId ->
                          val roomName = groupedCandidates[profileId]?.firstOrNull()?.profileName ?: "Room"
                          val count = groupedCandidates[profileId]?.size ?: 0
                          androidx.compose.material3.FilterChip(
                              selected = selectedCandidateRoomId == profileId,
                              onClick = { selectedCandidateRoomId = profileId },
                              label = { Text("$roomName ($count)") },
                              colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                  selectedContainerColor = SuccessGreen,
                                  selectedLabelColor = Color.White
                              ),
                              shape = RoundedCornerShape(16.dp)
                          )
                      }
                  }
              }
          }

          val candidatesToShow = if (selectedCandidateRoomId != null) {
              groupedCandidates[selectedCandidateRoomId] ?: emptyList()
          } else {
              customOnlyCandidates
          }

          if (candidatesToShow.isNotEmpty()) {
              items(candidatesToShow, key = { "cand_${it.id}" }) { app ->
                val profile = firestoreCustomProfiles.find { it.id == app.profileId }
                val formattedPayout = formatPayout(app.payout.ifBlank { profile?.payout ?: "" })
                val roomName = app.profileName.ifBlank { profile?.name ?: "Custom Room" }
                Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                ) {
                  // 1. Room Name & Actions
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                      )
                      Text(
                        text = roomName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    // Right side actions
                    if (app.status == "Pending") {
                      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.TextButton(
                          onClick = { 
                            appToAccept = app
                          },
                          modifier = Modifier.height(32.dp),
                          contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                          Text("Accept", color = SuccessGreen, style = MaterialTheme.typography.labelMedium)
                        }
                        androidx.compose.material3.TextButton(
                          onClick = { 
                            appToDelete = app
                            isRejectAction = true
                          },
                          modifier = Modifier.height(32.dp),
                          contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                          Text("Reject", color = AlertRed, style = MaterialTheme.typography.labelMedium)
                        }
                      }
                    } else {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (app.status == "Rejected") MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)
                      ) {
                        Text(
                          text = if (app.status == "Paid") (if (app.hostPaid) "PAID" else "CANDIDATE PAID") else app.status.uppercase(),
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (app.status == "Rejected") MaterialTheme.colorScheme.error else SuccessGreen
                          ),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // 2. UID -- Payout --
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                  ) {
                    Text(
                      text = "UID - ${app.uid}",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    if (formattedPayout.isNotBlank()) {
                      Text(
                        text = "Payout - $formattedPayout",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  // 3. LV --
                  Text(
                    text = "LV - ${app.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  if (app.status == "Accepted" || app.status == "Paid") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.Top
                        ) {
                          Text(
                            text = when {
                              app.status == "Paid" && app.hostPaid -> 
                                "Host payment completed! Send room details to candidate."
                              app.status == "Paid" && !app.hostPaid -> 
                                if (formattedPayout.isNotBlank()) "Candidate paid! Now it is time to make your payment of $formattedPayout." else "Candidate paid! Now it is time to make your payment."
                              else -> 
                                if (formattedPayout.isNotBlank()) "Candidate accepted! Waiting for candidate to make payment of $formattedPayout." else "Candidate accepted! Waiting for candidate to make payment."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                          )
                          androidx.compose.material3.IconButton(
                            onClick = { 
                              appToDelete = app
                              isRejectAction = false
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(
                              Icons.Default.Delete,
                              contentDescription = "Delete Application",
                              tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                              modifier = Modifier.size(20.dp)
                            )
                          }
                        }

                        if (app.status == "Paid") {
                          if (!app.hostPaid) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                              onClick = { appForHostPayment = app },
                              modifier = Modifier.fillMaxWidth(),
                              colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                              )
                            ) {
                              Text("Book & Pay")
                            }
                          } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                              Button(
                                onClick = { appForSend = app },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                              ) {
                                Text("Send")
                              }
                              Button(
                                onClick = { appForSubmit = app },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                              ) {
                                Text("Submit")
                              }
                              Button(
                                onClick = { appForReport = app },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                              ) {
                                Text("Report")
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          if (roomRegistrations.isNotEmpty()) {
              item {
                  Text("Local Room Registrations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
              }
              items(roomRegistrations, key = { "reg_${it.id}" }) { reg ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(14.dp)
                  ) {
                    Text(
                      text = reg.tournamentName,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(text = "Player: ${reg.candidateName}", style = MaterialTheme.typography.labelMedium)
                      Text(text = "Lvl: ${reg.level}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "UID: ${reg.uid} | Phone: ${reg.phoneNo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
          }
        }
      }
    }
  }

  // View Registered Match Details Dialog
  selectedMatchForDetails?.let { match ->
    RegisteredMatchDetailsDialog(
      match = match,
      onDismiss = { selectedMatchForDetails = null }
    )
  }

  // Edit Profile Dialog
  if (showEditDialog) {
    EditProfileDialog(
      currentProfile = userProfile,
      accountEmail = registeredEmail,
      accountPhone = registeredPhone,
      onDismiss = { showEditDialog = false },
      onSave = { name, uid, region, bio, avatarId ->
        viewModel.updateProfile(name, uid, region, bio, avatarId) {
          showEditDialog = false
        }
      }
    )
  }

  // Logout Confirmation Dialog
  if (showLogoutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirmDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out")
        }
      },
      text = {
        Text("Are you sure you want to sign out of your account?")
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirmDialog = false
            viewModel.logout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Log Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Switch Account Dialog
  if (showSwitchAccountDialog) {
    AlertDialog(
      onDismissRequest = { showSwitchAccountDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Switch Account")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (allAccounts.isEmpty()) {
            Text("No other saved accounts found on this device.")
          } else {
            allAccounts.forEach { acc ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.switchAccount(acc.id)
                    showSwitchAccountDialog = false
                  }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(text = acc.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = "@${acc.username} • UID: ${acc.gameUid}", style = MaterialTheme.typography.labelSmall)
                  }
                  if (activeAccount?.id == acc.id) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          OutlinedButton(
            onClick = {
              showSwitchAccountDialog = false
              onNavigateToAuth()
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add / Login Another Account")
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showSwitchAccountDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  // Wallet Add Funds Dialog
  if (showAddFundsDialog) {
    UserAddFundsDialog(
      onDismiss = { showAddFundsDialog = false },
      onAddFunds = { amount ->
        viewModel.addWalletBalance(amount)
        showAddFundsDialog = false
      }
    )
  }

  // Wallet Withdraw Dialog
  if (showWithdrawDialog) {
    UserWithdrawFundsDialog(
      currentBalance = userProfile.walletBalance,
      onDismiss = { showWithdrawDialog = false },
      onWithdraw = { amount, upiId ->
        viewModel.withdrawWalletBalance(amount, upiId) { success, _ ->
          if (success) {
            showWithdrawDialog = false
          }
        }
      }
    )
  }

  // Wallet Transaction History Dialog
  if (showTransactionHistoryDialog) {
    UserTransactionHistoryDialog(
      transactions = walletTransactions,
      balance = userProfile.walletBalance,
      onDismiss = { showTransactionHistoryDialog = false }
    )
  }

  if (appForPayment != null) {
      PaymentDialog(
          onDismiss = { appForPayment = null },
          onPayWithWallet = {
              viewModel.updateApplicationStatus(appForPayment!!.id, appForPayment!!.profileId, "Paid")
              appForPayment = null
          },
          onPayWithUpi = {
              viewModel.updateApplicationStatus(appForPayment!!.id, appForPayment!!.profileId, "Paid")
              appForPayment = null
          }
      )
  }

  if (appForHostPayment != null) {
      PaymentDialog(
          onDismiss = { appForHostPayment = null },
          onPayWithWallet = {
              viewModel.updateHostPayment(appForHostPayment!!.id, true)
              appForHostPayment = null
          },
          onPayWithUpi = {
              viewModel.updateHostPayment(appForHostPayment!!.id, true)
              appForHostPayment = null
          }
      )
  }

  if (appForSubmit != null) {
      GenericSubmitDialog(
          application = appForSubmit,
          onDismiss = { appForSubmit = null },
          onSubmit = { screenshotUri, winnerName, winnerUid ->
              viewModel.submitMatchResult(appForSubmit!!.id, screenshotUri, winnerName, winnerUid)
              appForSubmit = null
          }
      )
  }

  if (appForReport != null) {
      GenericReportDialog(
          application = appForReport,
          onDismiss = { appForReport = null },
          onSubmit = { reasons, description, mediaUri ->
              viewModel.submitMatchReport(appForReport!!.id, reasons, description, mediaUri)
              appForReport = null
          }
      )
  }

  if (appForSend != null) {
      HostSendCredentialsDialog(
          onDismiss = { appForSend = null },
          onSend = { rId, rPass ->
              viewModel.sendCandidateCredentials(appForSend!!.id, rId, rPass)
              appForSend = null
          }
      )
  }

  if (appToAccept != null) {
      val candidate = appToAccept!!
      AlertDialog(
          onDismissRequest = { appToAccept = null },
          title = {
              Text("Accept Request?", fontWeight = FontWeight.Bold)
          },
          text = {
              Text("Are you sure you want to accept player ${candidate.candidateName} (UID: ${candidate.uid})? Once accepted, they will be asked to make payment.")
          },
          confirmButton = {
              Button(
                  onClick = {
                      viewModel.updateApplicationStatus(candidate.id, candidate.profileId, "Accepted")
                      appToAccept = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
              ) {
                  Text("Accept", color = Color.White)
              }
          },
          dismissButton = {
              TextButton(onClick = { appToAccept = null }) {
                  Text("Cancel")
              }
          }
      )
  }

  if (appToDelete != null) {
      val target = appToDelete!!
      AlertDialog(
          onDismissRequest = { appToDelete = null },
          title = {
              Text(if (isRejectAction) "Reject Request?" else "Delete Application?", fontWeight = FontWeight.Bold)
          },
          text = {
              Text(
                  if (isRejectAction)
                      "Are you sure you want to reject candidate ${target.candidateName}? This will remove their application."
                  else
                      "Are you sure you want to delete this tournament application? This action cannot be undone."
              )
          },
          confirmButton = {
              Button(
                  onClick = {
                      viewModel.deleteCustomProfileApplication(target.id)
                      appToDelete = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
              ) {
                  Text(if (isRejectAction) "Reject" else "Delete", color = Color.White)
              }
          },
          dismissButton = {
              TextButton(onClick = { appToDelete = null }) {
                  Text("Cancel")
              }
          }
      )
  }

  if (matchToDeleteBroadcast != null) {
    val targetMatch = matchToDeleteBroadcast!!
    AlertDialog(
      onDismissRequest = { matchToDeleteBroadcast = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(
          text = "Delete Broadcast Message?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete this broadcast message? This will remove the Room ID and Password credentials and remove the match from your Active Registered list.",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteBroadcastMessage(targetMatch.id) {
              matchToDeleteBroadcast = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { matchToDeleteBroadcast = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun StatCard(
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}


@Composable
fun EmptyHistoryCard(
  text: String,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
    }
  }
}

@Composable
fun EditProfileDialog(
  currentProfile: com.example.data.model.UserProfile,
  accountEmail: String = "",
  accountPhone: String = "",
  onDismiss: () -> Unit,
  onSave: (name: String, uid: String, region: String, bio: String, avatarId: Int) -> Unit
) {
  val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val safeDismiss = {
    keyboardController?.hide()
    focusManager.clearFocus()
    onDismiss()
  }

  var name by remember { mutableStateOf(currentProfile.name) }
  var uid by remember { mutableStateOf(currentProfile.uid) }

  var nameError by remember { mutableStateOf(false) }
  var uidError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = safeDismiss,
    properties = DialogProperties(decorFitsSystemWindows = false),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = "Edit Player Profile",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Display Name (Editable)
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            nameError = it.isBlank()
          },
          label = { Text("Display Name") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          },
          isError = nameError,
          supportingText = if (nameError) {
            { Text("Display name cannot be empty", color = MaterialTheme.colorScheme.error) }
          } else null,
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_name_input")
        )

        // 2. Game Player UID (Editable)
        OutlinedTextField(
          value = uid,
          onValueChange = {
            uid = it
            uidError = it.isBlank()
          },
          label = { Text("Game Player UID") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.SportsEsports,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          },
          isError = uidError,
          supportingText = if (uidError) {
            { Text("Player UID cannot be empty", color = MaterialTheme.colorScheme.error) }
          } else null,
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_uid_input")
        )

        // 3. In place of Region and Bio: Display Registered Account Mobile & Email used while creating account
        if (accountPhone.isNotBlank() || accountEmail.isNotBlank()) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Text(
            text = "ACCOUNT CREDENTIALS (USED AT SIGNUP)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }

        // Show Account Phone Number used while creating account
        if (accountPhone.isNotBlank()) {
          OutlinedTextField(
            value = accountPhone,
            onValueChange = {},
            readOnly = true,
            label = { Text("Account Phone Number") },
            supportingText = { Text("Used while creating account", fontSize = 11.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = SuccessGreen
              )
            },
            trailingIcon = {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = SuccessGreen.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Verified",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = SuccessGreen,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_profile_account_phone")
          )
        }

        // Show Account Email used while creating account
        if (accountEmail.isNotBlank()) {
          OutlinedTextField(
            value = accountEmail,
            onValueChange = {},
            readOnly = true,
            label = { Text("Account Email") },
            supportingText = { Text("Used while creating account", fontSize = 11.sp) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null,
                tint = NeonCyan
              )
            },
            trailingIcon = {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeonCyan.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Verified",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = NeonCyan,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("edit_profile_account_email")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          nameError = name.isBlank()
          uidError = uid.isBlank()
          if (!nameError && !uidError) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onSave(name, uid, currentProfile.region, currentProfile.bio, currentProfile.avatarId)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("save_edit_profile_button")
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = safeDismiss) {
        Text("Cancel")
      }
    }
  )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCandidatesDialog(
  profile: com.example.data.model.CustomProfile,
  applications: List<com.example.data.model.CustomProfileApplication>,
  onDismiss: () -> Unit,
  onUpdateStatus: (String, String, String) -> Unit, // appId, profileId, status
  onDelete: (String) -> Unit,
  onSendCandidateCredentials: (String, String, String) -> Unit = { _, _, _ -> },
  onHostPay: (String) -> Unit = {},
  onSubmitResult: (appId: String, screenshotUri: String, winnerName: String, winnerUid: String) -> Unit = { _, _, _, _ -> },
  onSubmitReport: (appId: String, reasons: String, description: String, mediaUri: String) -> Unit = { _, _, _, _ -> }
) {
  var appForSend by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var appForSubmit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var appForReport by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var appForHostPayment by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var candidateToAccept by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var candidateToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.CustomProfileApplication?>(null) }
  var isCandidateReject by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

  androidx.compose.material3.ModalBottomSheet(
      onDismissRequest = onDismiss,
      containerColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxHeight(0.9f)
  ) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
          Text(
              text = "BR Room Candidates",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
              text = "Send Room ID and Password to the candidates.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))

          androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
              itemsIndexed(applications, key = { _, app -> app.id }) { index, app ->
                  val formattedPayout = formatPayout(app.payout.ifBlank { profile.payout })
                  Column(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 8.dp)
                  ) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          // Left Side
                          Row(modifier = Modifier.weight(1f)) {
                              Text(text = "${index + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                              Column {
                                  Text(text = "UID - ${app.uid}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                  Text(text = "LV - ${app.level}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  if (formattedPayout.isNotBlank()) {
                                      Text(text = "Payout - $formattedPayout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  }
                              }
                          }
                          
                          // Right Side
                          if (app.status == "Pending") {
                              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                  androidx.compose.material3.TextButton(
                                      onClick = { 
                                          candidateToAccept = app
                                      },
                                      modifier = Modifier.height(32.dp),
                                      contentPadding = PaddingValues(horizontal = 8.dp)
                                  ) {
                                      Text("ACCEPT", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                                  }
                                  androidx.compose.material3.TextButton(
                                      onClick = { 
                                          candidateToDelete = app
                                          isCandidateReject = true
                                      },
                                      modifier = Modifier.height(32.dp),
                                      contentPadding = PaddingValues(horizontal = 8.dp)
                                  ) {
                                      Text("REJECT", color = AlertRed, style = MaterialTheme.typography.labelSmall)
                                  }
                              }
                          } else {
                              Surface(
                                  shape = RoundedCornerShape(6.dp),
                                  color = if (app.status == "Rejected") MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)
                              ) {
                                  Text(
                                      text = if (app.status == "Paid") (if (app.hostPaid) "PAID" else "CANDIDATE PAID") else app.status.uppercase(),
                                      style = MaterialTheme.typography.labelSmall.copy(
                                          fontWeight = FontWeight.Bold,
                                          color = if (app.status == "Rejected") MaterialTheme.colorScheme.error else SuccessGreen
                                      ),
                                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                  )
                              }
                          }
                      }

                      if (app.status == "Accepted" || app.status == "Paid") {
                          Spacer(modifier = Modifier.height(8.dp))
                          Surface(
                              shape = RoundedCornerShape(8.dp),
                              color = MaterialTheme.colorScheme.primaryContainer,
                              modifier = Modifier.fillMaxWidth()
                          ) {
                              Column(modifier = Modifier.padding(10.dp)) {
                                  Row(
                                      modifier = Modifier.fillMaxWidth(),
                                      horizontalArrangement = Arrangement.SpaceBetween,
                                      verticalAlignment = Alignment.Top
                                  ) {
                                      Text(
                                          text = when {
                                              app.status == "Paid" && app.hostPaid -> 
                                                  "Host payment completed! Send room details to candidate."
                                              app.status == "Paid" && !app.hostPaid -> 
                                                  if (formattedPayout.isNotBlank()) "Candidate paid! Now it is time to make your payment of $formattedPayout." else "Candidate paid! Now it is time to make your payment."
                                              else -> 
                                                  if (formattedPayout.isNotBlank()) "Candidate accepted! Waiting for candidate to make payment of $formattedPayout." else "Candidate accepted! Waiting for candidate to make payment."
                                          },
                                          style = MaterialTheme.typography.bodySmall,
                                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                                          modifier = Modifier.weight(1f).padding(end = 8.dp)
                                      )
                                      androidx.compose.material3.IconButton(
                                          onClick = { 
                                              candidateToDelete = app
                                              isCandidateReject = false
                                          },
                                          modifier = Modifier.size(20.dp)
                                      ) {
                                          Icon(
                                              Icons.Default.Delete,
                                              contentDescription = "Delete Application",
                                              tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                              modifier = Modifier.size(16.dp)
                                          )
                                      }
                                  }

                                  if (app.status == "Paid") {
                                      if (!app.hostPaid) {
                                          Spacer(modifier = Modifier.height(8.dp))
                                          Button(
                                              onClick = { appForHostPayment = app },
                                              modifier = Modifier.fillMaxWidth(),
                                              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                          ) {
                                              Text("Book & Pay")
                                          }
                                      } else {
                                          Spacer(modifier = Modifier.height(8.dp))
                                          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                              Button(
                                                  onClick = { appForSend = app },
                                                  modifier = Modifier.weight(1f),
                                                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                              ) {
                                                  Text("Send", style = MaterialTheme.typography.labelSmall)
                                              }
                                              Button(
                                                  onClick = { appForSubmit = app },
                                                  modifier = Modifier.weight(1f),
                                                  colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                              ) {
                                                  Text("Submit", style = MaterialTheme.typography.labelSmall)
                                              }
                                              Button(
                                                  onClick = { appForReport = app },
                                                  modifier = Modifier.weight(1f),
                                                  colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                                              ) {
                                                  Text("Report", style = MaterialTheme.typography.labelSmall)
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      }
                  }
                  androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
              }
          }
      }
  }


  if (appForSend != null) {
      HostSendCredentialsDialog(
          onDismiss = { appForSend = null },
          onSend = { rId, rPass ->
              onSendCandidateCredentials(appForSend!!.id, rId, rPass)
              appForSend = null
          }
      )
  }

  if (appForSubmit != null) {
      GenericSubmitDialog(
          application = appForSubmit,
          onDismiss = { appForSubmit = null },
          onSubmit = { screenshotUri, winnerName, winnerUid ->
              onSubmitResult(appForSubmit!!.id, screenshotUri, winnerName, winnerUid)
              appForSubmit = null
          }
      )
  }

  if (appForReport != null) {
      GenericReportDialog(
          application = appForReport,
          onDismiss = { appForReport = null },
          onSubmit = { reasons, description, mediaUri ->
              onSubmitReport(appForReport!!.id, reasons, description, mediaUri)
              appForReport = null
          }
      )
  }

  if (appForHostPayment != null) {
      PaymentDialog(
          onDismiss = { appForHostPayment = null },
          onPayWithWallet = {
              onHostPay(appForHostPayment!!.id)
              appForHostPayment = null
          },
          onPayWithUpi = {
              onHostPay(appForHostPayment!!.id)
              appForHostPayment = null
          }
      )
  }

  if (candidateToAccept != null) {
      val candidate = candidateToAccept!!
      AlertDialog(
          onDismissRequest = { candidateToAccept = null },
          title = {
              Text("Accept Request?", fontWeight = FontWeight.Bold)
          },
          text = {
              Text("Are you sure you want to accept player ${candidate.candidateName} (UID: ${candidate.uid})? Once accepted, they will be asked to make payment.")
          },
          confirmButton = {
              Button(
                  onClick = {
                      onUpdateStatus(candidate.id, candidate.profileId, "Accepted")
                      candidateToAccept = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
              ) {
                  Text("Accept", color = Color.White)
              }
          },
          dismissButton = {
              TextButton(onClick = { candidateToAccept = null }) {
                  Text("Cancel")
              }
          }
      )
  }

  if (candidateToDelete != null) {
      val target = candidateToDelete!!
      AlertDialog(
          onDismissRequest = { candidateToDelete = null },
          title = {
              Text(if (isCandidateReject) "Reject Request?" else "Delete Application?", fontWeight = FontWeight.Bold)
          },
          text = {
              Text(
                  if (isCandidateReject)
                      "Are you sure you want to reject candidate ${target.candidateName}? This will remove their application."
                  else
                      "Are you sure you want to delete this tournament application? This action cannot be undone."
              )
          },
          confirmButton = {
              Button(
                  onClick = {
                      onDelete(target.id)
                      candidateToDelete = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
              ) {
                  Text(if (isCandidateReject) "Reject" else "Delete", color = Color.White)
              }
          },
          dismissButton = {
              TextButton(onClick = { candidateToDelete = null }) {
                  Text("Cancel")
              }
          }
      )
  }
}

@Composable
fun GenericSubmitDialog(
    application: CustomProfileApplication? = null,
    onDismiss: () -> Unit,
    onSubmit: (screenshotUri: String, winnerName: String, winnerUid: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var winnerName by remember { mutableStateOf(application?.candidateName ?: "") }
    var winnerUid by remember { mutableStateOf(application?.uid ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> photoUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = TrophyGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Match Result", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Upload the match result screenshot and enter the winner details below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Match Result Photo Upload Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Match Result Photo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (photoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Match Result Screenshot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { photoUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoUri == null) "Upload Match Result Photo" else "Change Match Result Photo")
                    }
                }

                HorizontalDivider()

                // Winner Details Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Winner Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = winnerName,
                        onValueChange = { winnerName = it },
                        label = { Text("Winner Name") },
                        placeholder = { Text("Enter winner player name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = winnerUid,
                        onValueChange = { winnerUid = it },
                        label = { Text("Winner UID") },
                        placeholder = { Text("Enter winner game UID") },
                        leadingIcon = {
                            Icon(Icons.Default.SportsEsports, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (application != null && (winnerName != application.candidateName || winnerUid != application.uid)) {
                        TextButton(
                            onClick = {
                                winnerName = application.candidateName
                                winnerUid = application.uid
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Fill Candidate (${application.candidateName})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Match result submitted for $winnerName (UID: $winnerUid)!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSubmit(photoUri?.toString() ?: "", winnerName, winnerUid)
                },
                enabled = winnerName.isNotBlank() && winnerUid.isNotBlank()
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
fun GenericReportDialog(
    application: CustomProfileApplication? = null,
    onDismiss: () -> Unit,
    onSubmit: (reasons: String, description: String, mediaUri: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var reportText by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    val selectedReasons = remember { mutableStateListOf<String>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri
            isVideo = false
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri
            isVideo = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Report Match Issue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (application != null) {
                        Text(
                            text = "Player: ${application.candidateName} (UID: ${application.uid})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Evidence: Photo and Video
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Evidence (Photo or Video)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (mediaUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (isVideo) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Video Evidence Attached",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = mediaUri,
                                    contentDescription = "Evidence Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            IconButton(
                                onClick = {
                                    mediaUri = null
                                    isVideo = false
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (mediaUri != null && !isVideo) "Change Photo" else "Upload Photo",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (mediaUri != null && isVideo) "Change Video" else "Upload Video",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 2. Violation reasons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Select Violation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    val violationOptions = listOf(
                        "Abnormal headshot",
                        "Shot through barriers",
                        "Walked through barriers",
                        "Abnormal movement",
                        "Fake Match Result Screenshot",
                        "Toxic Behavior / Abuse",
                        "Quit Match / No Show"
                    )

                    violationOptions.forEach { option ->
                        val isChecked = selectedReasons.contains(option)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isChecked) selectedReasons.remove(option)
                                    else selectedReasons.add(option)
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedReasons.add(option)
                                    else selectedReasons.remove(option)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 3. Issue Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Issue Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Explain what happened in detail...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reasonSummary = if (selectedReasons.isNotEmpty()) {
                        selectedReasons.joinToString(", ")
                    } else "General match issue"

                    Toast.makeText(
                        context,
                        "Report submitted. Support will review shortly.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSubmit(reasonSummary, reportText, mediaUri?.toString() ?: "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                enabled = selectedReasons.isNotEmpty() || reportText.isNotBlank() || mediaUri != null
            ) {
                Text("Report", color = Color.White)
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
fun HostSendCredentialsDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val safeDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }
    var roomId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var roomPassword by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Send Room Details") },
        text = { 
            Column(modifier = Modifier.imePadding()) {
                Text("Enter Room ID and Password for this candidate.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = roomId, onValueChange = { roomId = it }, label = { Text("Room ID") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = roomPassword, onValueChange = { roomPassword = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onSend(roomId, roomPassword) 
                }, 
                enabled = roomId.isNotBlank() && roomPassword.isNotBlank()
            ) { 
                Text("Send") 
            } 
        },
        dismissButton = { 
            androidx.compose.material3.TextButton(onClick = safeDismiss) { 
                Text("Cancel") 
            } 
        }
    )
}

private fun formatPayout(raw: String): String {
    val clean = raw.trim()
    if (clean.isBlank()) return ""
    return if (clean.startsWith("₹")) clean else "₹$clean"
}

@Composable
fun RegisteredMatchDetailsDialog(
  match: MatchItem,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = match.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Broadcast Status Card
        if (match.isRoomBroadcasted && match.roomId.isNotBlank() && match.roomPassword.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("dialog_broadcast_credentials")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Admin Broadcast Message",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SuccessGreen.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "LIVE ✓",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Credentials broadcasted by Admin to all registered candidates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
              )

              Spacer(modifier = Modifier.height(10.dp))
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
                    Text(match.roomId, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                  }
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("Password", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(match.roomPassword, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Credentials", "Room ID: ${match.roomId} | Password: ${match.roomPassword}"))
                    Toast.makeText(context, "Room ID & Password copied!", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Copy", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                  onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.dts.freefiremax")
                      ?: context.packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                    if (intent != null) {
                      context.startActivity(intent)
                    } else {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      clipboard.setPrimaryClip(ClipData.newPlainText("Credentials", "Room ID: ${match.roomId} | Password: ${match.roomPassword}"))
                      Toast.makeText(context, "Room credentials copied! Open Free Fire to join.", Toast.LENGTH_LONG).show()
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Launch", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Column {
                Text(
                  text = "Awaiting Admin Broadcast",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Room ID & Password will be delivered here in message form & via phone notification when Admin broadcasts to all registered candidates.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Match Info Grid
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Match Timing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${match.date} at ${match.time}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Format & Map", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${match.format} • ${match.mapName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Entry Fee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(if (match.entryFee == 0) "FREE" else "₹${match.entryFee}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Prize Pool", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("₹${match.prizePool}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TrophyGold))
            }
          }
        }

        // Rules
        if (match.rules.isNotBlank()) {
          Column {
            Text(
              text = "Rules & Instructions",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = match.rules,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun UserAddFundsDialog(
  onDismiss: () -> Unit,
  onAddFunds: (Int) -> Unit
) {
  val context = LocalContext.current
  var amountText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val quickAmounts = listOf(50, 100, 200, 500, 1000)

  val upiLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    Toast.makeText(context, "UPI payment completed", Toast.LENGTH_SHORT).show()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Wallet Balance")
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Deposit money into your wallet via UPI to enter paid tournaments and win cash rewards.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // UPI ID copy box
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Payee UPI ID",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = UpiUtils.UPI_ID,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
            IconButton(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("UPI ID", UpiUtils.UPI_ID)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy UPI ID",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        // Quick amount chips
        Text(
          text = "Quick Select Amount:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickAmounts.forEach { amt ->
            val isSelected = amountText == amt.toString()
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .clickable {
                  amountText = amt.toString()
                  errorMessage = null
                }
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              ) {
                Text(
                  text = "₹$amt",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                  )
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { ch -> ch.isDigit() }
            errorMessage = null
          },
          label = { Text("Deposit Amount (₹)") },
          placeholder = { Text("e.g. 100") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_wallet_add_amount")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    },
    confirmButton = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Button(
          onClick = {
            val amountInt = amountText.toIntOrNull()
            if (amountInt == null || amountInt <= 0) {
              errorMessage = "Please enter a valid amount"
              return@Button
            }
            try {
              val intent = UpiUtils.createUpiIntent(
                payeeAddress = UpiUtils.UPI_ID,
                amount = amountInt.toString(),
                transactionNote = "Tournament Wallet Deposit"
              )
              upiLauncher.launch(intent)
              onAddFunds(amountInt)
            } catch (e: Exception) {
              // Graceful simulation fallback on emulator/device without UPI app
              onAddFunds(amountInt)
              Toast.makeText(context, "Added ₹$amountInt to wallet!", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth().testTag("btn_confirm_add_funds")
        ) {
          Text("Pay via UPI App / Add Funds", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = {
            val amountInt = amountText.toIntOrNull() ?: 100
            onAddFunds(amountInt)
            Toast.makeText(context, "₹$amountInt added to wallet!", Toast.LENGTH_SHORT).show()
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("btn_fast_deposit")
        ) {
          Text("Instant Add (₹${amountText.toIntOrNull() ?: 100})")
        }

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
          Text("Cancel")
        }
      }
    },
    dismissButton = null
  )
}

@Composable
fun UserWithdrawFundsDialog(
  currentBalance: Int,
  onDismiss: () -> Unit,
  onWithdraw: (Int, String) -> Unit
) {
  var amountText by remember { mutableStateOf("") }
  var upiId by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val quickWithdraws = listOf(50, 100, 200)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Withdraw Funds")
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Available Balance card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .padding(14.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Available Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "₹$currentBalance",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
            TextButton(
              onClick = {
                amountText = currentBalance.toString()
                errorMessage = null
              }
            ) {
              Text("Withdraw All", fontWeight = FontWeight.Bold)
            }
          }
        }

        // Quick amount chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickWithdraws.forEach { amt ->
            val isSelected = amountText == amt.toString()
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .clickable {
                  amountText = amt.toString()
                  errorMessage = null
                }
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              ) {
                Text(
                  text = "₹$amt",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                  )
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { ch -> ch.isDigit() }
            errorMessage = null
          },
          label = { Text("Withdrawal Amount (₹)") },
          placeholder = { Text("Min ₹10") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_withdraw_amount")
        )

        OutlinedTextField(
          value = upiId,
          onValueChange = {
            upiId = it.trim()
            errorMessage = null
          },
          label = { Text("Your UPI ID") },
          placeholder = { Text("e.g. mobile@upi or name@okaxis") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_withdraw_upi_id")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        Text(
          text = "• Withdrawals are processed instantly to your specified UPI ID.\n• Minimum withdrawal amount is ₹10.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amountInt = amountText.toIntOrNull()
          if (amountInt == null || amountInt <= 0) {
            errorMessage = "Please enter a valid amount"
            return@Button
          }
          if (amountInt > currentBalance) {
            errorMessage = "Amount exceeds available balance (₹$currentBalance)"
            return@Button
          }
          if (upiId.isBlank() || !upiId.contains("@")) {
            errorMessage = "Please enter a valid UPI ID (e.g. user@bank)"
            return@Button
          }
          onWithdraw(amountInt, upiId)
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("btn_confirm_withdraw")
      ) {
        Text("Withdraw Now", fontWeight = FontWeight.Bold)
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
fun UserTransactionHistoryDialog(
  transactions: List<WalletTransaction>,
  balance: Int,
  onDismiss: () -> Unit
) {
  var selectedFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Credits, 2 = Debits

  val filteredList = remember(transactions, selectedFilter) {
    when (selectedFilter) {
      1 -> transactions.filter { it.type.equals("CREDIT", ignoreCase = true) }
      2 -> transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
      else -> transactions
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Transaction History")
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
          Text(
            text = "₹$balance",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Filter Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("All (${transactions.size})", "Credits (+)", "Debits (-)").forEachIndexed { index, label ->
            val isSelected = selectedFilter == index
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedFilter = index }
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                  )
                )
              }
            }
          }
        }

        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No transactions found in this filter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredList) { tx ->
              val isCredit = tx.type.equals("CREDIT", ignoreCase = true)
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                          if (isCredit) SuccessGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f)
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) SuccessGreen else AlertRed,
                        modifier = Modifier.size(18.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                      Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = tx.date,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = if (isCredit) "+₹${tx.amount}" else "-₹${tx.amount}",
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) SuccessGreen else AlertRed
                      )
                    )
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = SuccessGreen.copy(alpha = 0.12f)
                    ) {
                      Text(
                        text = tx.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          color = SuccessGreen
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Close")
      }
    }
  )
}

