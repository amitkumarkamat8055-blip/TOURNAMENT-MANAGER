package com.example.ui.matches

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.utils.UpiUtils
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import com.example.utils.RoomBroadcastManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MatchItem
import com.example.data.model.PrizeBreakdown
import com.example.data.model.isDailyMatch
import com.example.data.model.getEffectivePerKill
import com.example.ui.admin.SendRoomIdDialog
import com.example.ui.components.MatchNumberBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailsScreen(
  matchId: Long,
  viewModel: TournamentViewModel,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val allMatches by viewModel.filteredMatches.collectAsStateWithLifecycle()
  val selectedMatchFromVm by viewModel.selectedMatch.collectAsStateWithLifecycle()
  val currentMatch = allMatches.find { it.id == matchId } ?: selectedMatchFromVm?.takeIf { it.id == matchId } ?: return
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val allRegistrations by viewModel.allMatchRegistrations.collectAsStateWithLifecycle()

  var showJoinDialog by remember { mutableStateOf(false) }
  var showCancelDialog by remember { mutableStateOf(false) }
  var showBroadcastDialog by remember { mutableStateOf(false) }
  var showDeleteBroadcastConfirm by remember { mutableStateOf(false) }
  var showEditRulesDialog by remember { mutableStateOf(false) }
  var showEditPrizeDialog by remember { mutableStateOf(false) }
  var showEditLevelDialog by remember { mutableStateOf(false) }
  var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(1000)
      currentTimeMillis = System.currentTimeMillis()
    }
  }

  var gameNameInput by remember { mutableStateOf(userProfile.name) }
  var gameNameError by remember { mutableStateOf(false) }
  var gameUidInput by remember { mutableStateOf(userProfile.uid) }
  var gameUidError by remember { mutableStateOf(false) }

  val upiLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    // In a real app, you would verify the transaction ID with your backend here.
    // For now, if the UPI intent returns, we assume success or user cancellation and handle via backend.
    // Here we'll simulate success for demonstration if result is OK.
    if (result.resultCode == android.app.Activity.RESULT_OK) {
      viewModel.joinMatch(
        matchId = currentMatch.id,
        inGameUid = gameUidInput.trim(),
        entryFee = 0, // Deduct 0 from wallet since paid via UPI
        candidateName = gameNameInput.trim().ifBlank { userProfile.name },
        paymentMethod = "UPI"
      ) { success ->
        if (success) {
          showJoinDialog = false
          Toast.makeText(context, "UPI Payment Successful & Joined!", Toast.LENGTH_SHORT).show()
        }
      }
    } else {
      Toast.makeText(context, "UPI Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(matchId) {
    viewModel.selectMatch(matchId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = currentMatch?.name ?: "Match Details",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
            if (currentMatch != null) {
              Text(
                text = "MATCH #${currentMatch.matchNumber} • ${currentMatch.gameTitle}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("details_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        actions = {
          IconButton(onClick = {
            val sendIntent = android.content.Intent().apply {
              action = android.content.Intent.ACTION_SEND
              putExtra(
                android.content.Intent.EXTRA_TEXT,
                "Check out this tournament: ${currentMatch?.name} with prize pool of ₹${currentMatch?.prizePool}!"
              )
              type = "text/plain"
            }
            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Tournament"))
          }) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      )
    },
    bottomBar = {
      if (currentMatch != null) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Entry Fee",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = if (currentMatch.entryFee == 0) "FREE" else "₹${currentMatch.entryFee}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Black,
                  color = if (currentMatch.entryFee == 0) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
              )
            }

            if (isAdmin) {
              Button(
                onClick = { showBroadcastDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("admin_bottom_broadcast_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Campaign,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (currentMatch.isRoomBroadcasted) "RE-BROADCAST ROOM" else "BROADCAST ROOM ID",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  )
                )
              }
            } else if (currentMatch.isJoined) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { showCancelDialog = true },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AlertRed
                  ),
                  border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                  modifier = Modifier.testTag("cancel_registration_button")
                ) {
                  Text("Cancel")
                }

                Button(
                  onClick = {
                    if (currentMatch.isRoomBroadcasted && currentMatch.roomId.isNotBlank()) {
                      copyToClipboard(context, "Room Credentials", "Room ID: ${currentMatch.roomId} | Password: ${currentMatch.roomPassword}")
                      Toast.makeText(context, "Room ID: ${currentMatch.roomId}, Password: ${currentMatch.roomPassword} copied!", Toast.LENGTH_SHORT).show()
                    } else {
                      Toast.makeText(context, "Room credentials will activate when Admin broadcasts to all registered candidates!", Toast.LENGTH_SHORT).show()
                    }
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentMatch.isRoomBroadcasted) MaterialTheme.colorScheme.primary else SuccessGreen,
                    contentColor = Color.White
                  ),
                  modifier = Modifier.testTag("room_ready_button")
                ) {
                  Icon(
                    imageVector = if (currentMatch.isRoomBroadcasted) Icons.Default.Campaign else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(if (currentMatch.isRoomBroadcasted) "ROOM BROADCASTED ✓" else "REGISTERED ✓")
                }
              }
            } else {
              Button(
                onClick = {
                  gameNameInput = userProfile.name.ifBlank { "Player" }
                  gameUidInput = userProfile.uid
                  gameNameError = false
                  gameUidError = false
                  showJoinDialog = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("details_join_match_button")
              ) {
                Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "JOIN MATCH",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                  )
                )
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    if (currentMatch == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Text("Loading Match Information...", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      Column(
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Admin Quick Edit Tools Strip (Strictly visible only for Admin)
        if (isAdmin) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_controls_banner")
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Admin Edit Tools",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Text(
                  text = "Manual Edit Mode",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { showEditPrizeDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_action_edit_prizes"),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrophyGold)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Prizes", fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(
                  onClick = { showEditRulesDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_action_edit_rules"),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Rules", fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(
                  onClick = { showEditLevelDialog = true },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_action_edit_level"),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Level Req", fontSize = 12.sp, maxLines = 1)
                }
              }
            }
          }
        }

        // Broadcast Alert Banner for Joined Candidates
        if (currentMatch.isJoined && currentMatch.isRoomBroadcasted && currentMatch.roomId.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("details_broadcast_alert")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "ADMIN BROADCAST: ROOM LIVE!",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Room ID: ${currentMatch.roomId}  •  Password: ${currentMatch.roomPassword}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Admin broadcasted to all candidates. Enter Free Fire MAX now to join.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
              }
            }
          }
        } else if (currentMatch.isJoined) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("details_awaiting_broadcast_alert")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "REGISTERED • AWAITING BROADCAST",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Room ID & Password will be delivered here in message form & via notification when Admin broadcasts to all registered candidates.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Hero Match Profile Card with Esports Banner
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // Large Esports Header Banner
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                  Brush.linearGradient(
                    colors = listOf(
                      Color(0xFF0A1F13), // deep tactical green
                      Color(0xFF143E24), // esports emerald
                      Color(0xFF0D1811)  // dark tactical
                    )
                  )
                )
            ) {
              if (currentMatch.bannerImageUrl.isNotBlank()) {
                AsyncImage(
                  model = currentMatch.bannerImageUrl,
                  contentDescription = "Match Profile Banner",
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
                  tint = Color.White.copy(alpha = 0.08f),
                  modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 4.dp)
                )
              }

              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                // Top Row: Match #, Free Fire MAX badge, Status
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    MatchNumberBadge(number = currentMatch.matchNumber)

                    // Prominent Free Fire MAX Badge
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = Color(0xFFE65100).copy(alpha = 0.35f),
                      border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.55f))
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Whatshot,
                          contentDescription = null,
                          tint = Color(0xFFFF9800),
                          modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "FREE FIRE MAX",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                          ),
                          color = Color(0xFFFFCC80)
                        )
                      }
                    }
                  }

                  StatusBadge(status = currentMatch.status)
                }

                // Bottom Row in Banner: Map & Format Frosted Pill
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color.Black.copy(alpha = 0.45f),
                  border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = SuccessGreen,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = "${currentMatch.mapName}  •  ${currentMatch.format}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                      ),
                      color = Color.White
                    )
                  }
                }
              }
            }

            // Body info
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = currentMatch.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Quick Specs Row (Entry, Prize Pool, Per Kill for Daily only, Format)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = "ENTRY",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = if (currentMatch.entryFee == 0) "FREE" else "₹${currentMatch.entryFee}",
                      style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (currentMatch.entryFee == 0) SuccessGreen else MaterialTheme.colorScheme.onSurface
                      )
                    )
                  }

                  Box(
                    modifier = Modifier
                      .width(1.dp)
                      .height(24.dp)
                      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                  )

                  Column {
                    Text(
                      text = "PRIZE POOL",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "₹${currentMatch.prizePool}",
                      style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = TrophyGold
                      )
                    )
                  }

                  // PER KILL: Strictly ONLY displayed in Daily Matches
                  if (currentMatch.isDailyMatch) {
                    Box(
                      modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Column {
                      Text(
                        text = "PER KILL",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                        color = TrophyGold
                      )
                      Text(
                        text = "₹${currentMatch.getEffectivePerKill()}",
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.Black,
                          color = TrophyGold
                        )
                      )
                    }
                  }

                  Box(
                    modifier = Modifier
                      .width(1.dp)
                      .height(24.dp)
                      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                  )

                  Column {
                    Text(
                      text = "FORMAT",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = currentMatch.format,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Progress Bar
              val progress = (currentMatch.totalPlayers.toFloat() / currentMatch.maxPlayers.toFloat()).coerceIn(0f, 1f)
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Lobby Capacity: ${currentMatch.totalPlayers}/${currentMatch.maxPlayers} Players",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${(progress * 100).toInt()}% Full",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (progress > 0.85f) AlertRed else SuccessGreen
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                  color = if (progress > 0.85f) AlertRed else SuccessGreen,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant,
                  strokeCap = StrokeCap.Round
                )
              }
            }
          }
        }

        // SCHEDULE: Simple, clean, unified Date and Time card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Day
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Event,
                  contentDescription = "Match Day",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
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
                  text = currentMatch.date,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = SuccessGreen
                )
              }
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            )

            // Time
            Row(
              modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = "Match Time",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
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
                  text = currentMatch.time,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = TrophyGold
                )
              }
            }
          }
        }

        // Info Cards Row: If Daily match -> [Per Kill Reward] & [Level Requirement]
        // If Weekly match -> [Level Requirement] & [Server Region] (NO Per Kill in weekly)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (currentMatch.isDailyMatch) {
            // Per Kill Reward Card (Exclusive to Daily Matches) - clean simple surface
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = TrophyGold,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Per Kill Reward",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "₹${currentMatch.getEffectivePerKill()}",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                  ),
                  color = TrophyGold
                )
                Text(
                  text = "Cash Per Fragger",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Rank Card
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.MilitaryTech,
                      contentDescription = null,
                      tint = NeonCyan,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Level Requirement",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  if (isAdmin) {
                    IconButton(
                      onClick = { showEditLevelDialog = true },
                      modifier = Modifier.size(24.dp).testTag("admin_edit_level_req_btn")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Level Requirement",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = currentMatch.rankRequirement,
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                  color = NeonCyan
                )
                Text(
                  text = "Min Player Level",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            // Rank Card
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.MilitaryTech,
                      contentDescription = null,
                      tint = NeonCyan,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Level Requirement",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  if (isAdmin) {
                    IconButton(
                      onClick = { showEditLevelDialog = true },
                      modifier = Modifier.size(24.dp).testTag("admin_edit_level_req_btn_alt")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Level Requirement",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = currentMatch.rankRequirement,
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                  color = NeonCyan
                )
                Text(
                  text = "Min Player Level",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Region Card
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Server Region",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = currentMatch.region,
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Official Server",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        // Room ID & Password Card (Stateful)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (currentMatch.isJoined)
              ElectricIndigo.copy(alpha = 0.12f)
            else
              MaterialTheme.colorScheme.surface
          ),
          border = BorderStroke(
            1.dp,
            if (currentMatch.isJoined) ElectricIndigo.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (currentMatch.isJoined || isAdmin) Icons.Default.Key else Icons.Default.Lock,
                  contentDescription = null,
                  tint = if (currentMatch.isJoined || isAdmin) ElectricIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Custom Room Access",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              if (isAdmin) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (currentMatch.isRoomBroadcasted) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = if (currentMatch.isRoomBroadcasted) "BROADCASTED ✓" else "NOT BROADCASTED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = if (currentMatch.isRoomBroadcasted) SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              } else if (currentMatch.isJoined) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (currentMatch.isRoomBroadcasted) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = if (currentMatch.isRoomBroadcasted) "BROADCAST LIVE" else "AWAITING BROADCAST",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = if (currentMatch.isRoomBroadcasted) SuccessGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAdmin) {
              // Admin view & controls
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  RoomInfoBox(
                    label = "Room ID",
                    value = currentMatch.roomId.ifBlank { "Not set" },
                    onCopy = {
                      if (currentMatch.roomId.isNotBlank()) copyToClipboard(context, "Room ID", currentMatch.roomId)
                    },
                    modifier = Modifier.weight(1f)
                  )
                  RoomInfoBox(
                    label = "Password",
                    value = currentMatch.roomPassword.ifBlank { "Not set" },
                    onCopy = {
                      if (currentMatch.roomPassword.isNotBlank()) copyToClipboard(context, "Password", currentMatch.roomPassword)
                    },
                    modifier = Modifier.weight(1f)
                  )
                }

                Button(
                  onClick = { showBroadcastDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_details_broadcast_button")
                ) {
                  Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = if (currentMatch.isRoomBroadcasted) "Re-Broadcast Room ID to All Candidates" else "Broadcast Room ID & Password to Candidates",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }
              }
            } else if (currentMatch.isJoined) {
              val broadcastTime = RoomBroadcastManager.getBroadcastTime(context, currentMatch.id)
              if (currentMatch.isRoomBroadcasted && broadcastTime == 0L) {
                RoomBroadcastManager.recordBroadcastTime(context, currentMatch.id, currentTimeMillis)
              }

              val isBroadcastDeleted = RoomBroadcastManager.isBroadcastDeleted(context, currentMatch.id)
              val isBroadcastExpired = RoomBroadcastManager.isBroadcastExpired(context, currentMatch.id, currentTimeMillis)
              val remainingMillis = RoomBroadcastManager.getRemainingMillis(context, currentMatch.id, currentTimeMillis)
              val remainingFormatted = RoomBroadcastManager.formatRemainingTime(remainingMillis)

              if (currentMatch.isRoomBroadcasted && currentMatch.roomId.isNotBlank() && currentMatch.roomPassword.isNotBlank()) {
                if (!isBroadcastDeleted && !isBroadcastExpired) {
                  // Admin Broadcast message form container for registered candidate
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("details_broadcasted_credentials")
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
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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

                          // 30-minute auto-delete countdown chip
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

                          // Manual delete icon button
                          IconButton(
                            onClick = { showDeleteBroadcastConfirm = true },
                            modifier = Modifier
                              .size(26.dp)
                              .testTag("details_delete_broadcast_button")
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
                        text = "Admin has broadcasted your Custom Room ID & Password to all registered candidates! Use credentials below to join the match room in Free Fire MAX.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                          text = "Auto-deletes in 30 minutes (remaining: $remainingFormatted) • Tap trash icon to delete now",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                          color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                      }

                      Spacer(modifier = Modifier.height(12.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        RoomInfoBox(
                          label = "Room ID",
                          value = currentMatch.roomId,
                          onCopy = {
                            copyToClipboard(context, "Room ID", currentMatch.roomId)
                            Toast.makeText(context, "Room ID copied: ${currentMatch.roomId}", Toast.LENGTH_SHORT).show()
                          },
                          modifier = Modifier.weight(1f)
                        )
                        RoomInfoBox(
                          label = "Password",
                          value = currentMatch.roomPassword,
                          onCopy = {
                            copyToClipboard(context, "Password", currentMatch.roomPassword)
                            Toast.makeText(context, "Password copied: ${currentMatch.roomPassword}", Toast.LENGTH_SHORT).show()
                          },
                          modifier = Modifier.weight(1f)
                        )
                      }

                      Spacer(modifier = Modifier.height(10.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        OutlinedButton(
                          onClick = {
                            copyToClipboard(context, "Room Credentials", "Room ID: ${currentMatch.roomId} | Password: ${currentMatch.roomPassword}")
                            Toast.makeText(context, "Room ID & Password copied to clipboard!", Toast.LENGTH_SHORT).show()
                          },
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier.weight(1f)
                        ) {
                          Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Copy Both", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                          onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.dts.freefiremax")
                              ?: context.packageManager.getLaunchIntentForPackage("com.dts.freefireth")
                            if (intent != null) {
                              context.startActivity(intent)
                            } else {
                              copyToClipboard(context, "Room Credentials", "Room ID: ${currentMatch.roomId} | Password: ${currentMatch.roomPassword}")
                              Toast.makeText(context, "Room ID & Password copied! Open Free Fire to join.", Toast.LENGTH_LONG).show()
                            }
                          },
                          shape = RoundedCornerShape(8.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                          modifier = Modifier.weight(1f)
                        ) {
                          Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Launch Game", style = MaterialTheme.typography.labelSmall)
                        }
                      }
                    }
                  }
                } else {
                  // Broadcast expired (30 min limit) or deleted manually
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("details_expired_broadcast")
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
                        onClick = { showDeleteBroadcastConfirm = true },
                        modifier = Modifier.size(32.dp)
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
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.AccessTime,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(22.dp)
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
            } else {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("locked_credentials_card")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                  )
                  Column {
                    Text(
                      text = "Credentials Encrypted & Locked",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Room ID and Password are only visible to candidates who have registered for this match. Join match to receive credentials when Admin broadcasts.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }

        // Prize Distribution Section
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = null,
                  tint = TrophyGold,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Prize Distribution",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Pool: ₹${currentMatch.prizePool}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = TrophyGold
                  )
                )
                if (isAdmin) {
                  Spacer(modifier = Modifier.width(6.dp))
                  IconButton(
                    onClick = { showEditPrizeDialog = true },
                    modifier = Modifier.size(28.dp).testTag("admin_edit_prize_distribution_btn")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "Edit Prize Distribution",
                      tint = TrophyGold,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Displayed Prizes: In Weekly matches, strictly filter out any Per Kill entry
            val displayedPrizes = currentMatch.prizeDistribution.filterNot { 
              it.rank.contains("Per Kill", ignoreCase = true) 
            }

            displayedPrizes.forEachIndexed { index, prize ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  val iconColor = when (index) {
                    0 -> TrophyGold
                    1 -> Color(0xFFC0C0C0)
                    2 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.primary
                  }
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${index + 1}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = iconColor
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = prize.rank,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Text(
                  text = "₹${prize.amount}",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TrophyGold
                  )
                )
              }
              if (index < displayedPrizes.lastIndex) {
                HorizontalDivider(
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                  modifier = Modifier.padding(vertical = 2.dp)
                )
              }
            }

            // PER KILL BOUNTY: Highlighted card strictly ONLY in Daily Matches!
            if (currentMatch.isDailyMatch) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2B210B),
                border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.65f)),
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
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TrophyGold.copy(alpha = 0.2f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Per Kill Bonus",
                        tint = TrophyGold,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "Daily Match Kill Bonus",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                      )
                      Text(
                        text = "Added directly to wallet per confirmed kill",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                      )
                    }
                  }
                  Text(
                    text = "₹${currentMatch.getEffectivePerKill()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      color = TrophyGold
                    )
                  )
                }
              }
            }
          }
        }

        // Rules & Instructions
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Tournament Rules & Policy",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              if (isAdmin) {
                IconButton(
                  onClick = { showEditRulesDialog = true },
                  modifier = Modifier.size(28.dp).testTag("admin_edit_rules_btn")
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Rules & Policy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = currentMatch.rules,
              style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "About this Match:",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = currentMatch.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Participants Preview Section
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Groups,
                  contentDescription = null,
                  tint = NeonCyan,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Registered Players (${currentMatch.totalPlayers})",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Player Roster rows
            val roster = listOf(
              Triple("Viper_99", "UID: 9812401", "Squad Leader"),
              Triple("PhantomSniper", "UID: 4421098", "Assault"),
              Triple("BlazeFury", "UID: 6732910", "Support"),
              Triple(if (currentMatch.isJoined) userProfile.name else "GhostRider_X", if (currentMatch.isJoined) "UID: ${currentMatch.userGameUid.ifBlank { userProfile.uid }}" else "UID: 8812903", if (currentMatch.isJoined) "YOU (Registered)" else "Fragger")
            )

            roster.forEachIndexed { i, (pName, pUid, pRole) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(if (pRole.startsWith("YOU")) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = if (pRole.startsWith("YOU")) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = pName,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (pRole.startsWith("YOU")) FontWeight.Bold else FontWeight.Medium
                      ),
                      color = if (pRole.startsWith("YOU")) SuccessGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = pUid,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (pRole.startsWith("YOU")) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = pRole,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (pRole.startsWith("YOU")) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Join Match Confirmation Dialog
  if (showJoinDialog && currentMatch != null) {
    AlertDialog(
      onDismissRequest = { showJoinDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Match Registration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          IconButton(
            onClick = { showJoinDialog = false },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "${currentMatch.name} (Match #${currentMatch.matchNumber})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${currentMatch.gameTitle} • ${currentMatch.format} • ${currentMatch.mapName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 4.dp)
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Entry Fee:",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                  text = if (currentMatch.entryFee == 0) "FREE" else "₹${currentMatch.entryFee}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                )
              }
            }
          }

          OutlinedTextField(
            value = gameNameInput,
            onValueChange = {
              gameNameInput = it
              gameNameError = it.isBlank()
            },
            label = { Text("In-Game Name") },
            placeholder = { Text("e.g. ProSniper_99") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
            },
            isError = gameNameError,
            supportingText = {
              if (gameNameError) {
                Text("Please enter your in-game name")
              } else {
                Text("Your character display name in game")
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("join_game_name_input")
          )

          OutlinedTextField(
            value = gameUidInput,
            onValueChange = {
              gameUidInput = it
              gameUidError = it.isBlank()
            },
            label = { Text("In-Game Player UID") },
            placeholder = { Text("e.g. 548291047") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
            },
            isError = gameUidError,
            supportingText = {
              if (gameUidError) {
                Text("Please enter your in-game UID")
              } else {
                Text("Must match your game character ID for score tracking")
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("join_game_uid_input")
          )
        }
      },
      confirmButton = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (currentMatch.entryFee > 0) {
            Button(
              onClick = {
                val trimmedName = gameNameInput.trim()
                val trimmedUid = gameUidInput.trim()
                var hasError = false
                if (trimmedName.isBlank()) {
                  gameNameError = true
                  hasError = true
                }
                if (trimmedUid.isBlank()) {
                  gameUidError = true
                  hasError = true
                }
                if (hasError) return@Button

                try {
                  val intent = com.example.utils.UpiUtils.createUpiIntent(
                    amount = currentMatch.entryFee.toString(),
                    transactionNote = "Match ${currentMatch.matchNumber} Entry"
                  )
                  upiLauncher.launch(intent)
                } catch (e: Exception) {
                  viewModel.joinMatch(
                    matchId = currentMatch.id,
                    inGameUid = trimmedUid,
                    entryFee = 0,
                    candidateName = trimmedName,
                    paymentMethod = "UPI"
                  ) { success ->
                    if (success) {
                      showJoinDialog = false
                      Toast.makeText(context, "Registration Successful via UPI!", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_join_upi_button")
            ) {
              Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Pay with UPI (₹${currentMatch.entryFee})",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = {
                val trimmedName = gameNameInput.trim()
                val trimmedUid = gameUidInput.trim()
                var hasError = false
                if (trimmedName.isBlank()) {
                  gameNameError = true
                  hasError = true
                }
                if (trimmedUid.isBlank()) {
                  gameUidError = true
                  hasError = true
                }
                if (hasError) return@OutlinedButton

                if (userProfile.walletBalance < currentMatch.entryFee) {
                  Toast.makeText(
                    context,
                    "Insufficient Wallet Balance (₹${userProfile.walletBalance})",
                    Toast.LENGTH_SHORT
                  ).show()
                  return@OutlinedButton
                }

                viewModel.joinMatch(
                  matchId = currentMatch.id,
                  inGameUid = trimmedUid,
                  entryFee = currentMatch.entryFee,
                  candidateName = trimmedName,
                  paymentMethod = "Wallet"
                ) { success ->
                  if (success) {
                    showJoinDialog = false
                    Toast.makeText(context, "Registration Successful from Wallet!", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_join_wallet_button")
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Pay from Wallet (₹${currentMatch.entryFee})",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
          } else {
            Button(
              onClick = {
                val trimmedName = gameNameInput.trim()
                val trimmedUid = gameUidInput.trim()
                var hasError = false
                if (trimmedName.isBlank()) {
                  gameNameError = true
                  hasError = true
                }
                if (trimmedUid.isBlank()) {
                  gameUidError = true
                  hasError = true
                }
                if (hasError) return@Button

                viewModel.joinMatch(
                  matchId = currentMatch.id,
                  inGameUid = trimmedUid,
                  entryFee = 0,
                  candidateName = trimmedName,
                  paymentMethod = "Free"
                ) { success ->
                  if (success) {
                    showJoinDialog = false
                    Toast.makeText(context, "Registered for Match Successfully!", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_join_free_button")
            ) {
              Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Join for Free",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          TextButton(
            onClick = { showJoinDialog = false },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cancel_join_match_dialog_button")
          ) {
            Text(
              text = "Cancel",
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    )
  }

  // Cancel Match Dialog
  if (showCancelDialog && currentMatch != null) {
    AlertDialog(
      onDismissRequest = { showCancelDialog = false },
      title = { Text("Cancel Match Registration") },
      text = {
        Text("Are you sure you want to cancel your slot for this match? Your entry fee of ₹${currentMatch.entryFee} will be refunded to your wallet.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.cancelMatch(currentMatch.id, currentMatch.entryFee)
            showCancelDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
          modifier = Modifier.testTag("confirm_cancel_match_button")
        ) {
          Text("Yes, Cancel Slot")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelDialog = false }) {
          Text("Keep Slot")
        }
      }
    )
  }

  // Admin Broadcast Room ID Dialog
  if (showBroadcastDialog && currentMatch != null) {
    val registrations = allRegistrations.filter { it.matchId == currentMatch.id }
    SendRoomIdDialog(
      match = currentMatch,
      registeredCandidatesCount = registrations.size,
      onDismiss = { showBroadcastDialog = false },
      onBroadcast = { roomId, password ->
        viewModel.broadcastRoomCredentials(currentMatch.id, roomId, password) {
          showBroadcastDialog = false
          Toast.makeText(context, "Room ID & Password broadcasted to ${registrations.size} candidate(s)!", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  // Delete Broadcast Message Confirmation Dialog
  if (showDeleteBroadcastConfirm && currentMatch != null) {
    AlertDialog(
      onDismissRequest = { showDeleteBroadcastConfirm = false },
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
            viewModel.deleteBroadcastMessage(currentMatch.id) {
              showDeleteBroadcastConfirm = false
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
        TextButton(onClick = { showDeleteBroadcastConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Admin Edit Rules & Regulation Dialog
  if (showEditRulesDialog && currentMatch != null && isAdmin) {
    EditRulesAndRegulationDialog(
      currentRules = currentMatch.rules,
      currentDescription = currentMatch.description,
      onDismiss = { showEditRulesDialog = false },
      onSave = { updatedRules, updatedDesc ->
        showEditRulesDialog = false
        val updatedMatch = currentMatch.copy(
          rules = updatedRules,
          description = updatedDesc
        )
        viewModel.updateMatch(updatedMatch)
        Toast.makeText(context, "Tournament Rules & Policy updated!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Admin Edit Prize Distribution Dialog
  if (showEditPrizeDialog && currentMatch != null && isAdmin) {
    EditPrizeDistributionDialog(
      currentPrizePool = currentMatch.prizePool,
      currentPerKill = currentMatch.perKill,
      isDailyMatch = currentMatch.isDailyMatch,
      currentDistribution = currentMatch.prizeDistribution,
      onDismiss = { showEditPrizeDialog = false },
      onSave = { updatedPool, updatedKill, updatedDist ->
        showEditPrizeDialog = false
        val updatedMatch = currentMatch.copy(
          prizePool = updatedPool,
          perKill = updatedKill,
          prizeDistribution = updatedDist
        )
        viewModel.updateMatch(updatedMatch)
        Toast.makeText(context, "Prize Distribution updated!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Admin Edit Level Requirement Dialog
  if (showEditLevelDialog && currentMatch != null && isAdmin) {
    EditLevelRequirementDialog(
      currentLevelReq = currentMatch.rankRequirement,
      onDismiss = { showEditLevelDialog = false },
      onSave = { updatedLevelReq ->
        showEditLevelDialog = false
        val updatedMatch = currentMatch.copy(
          rankRequirement = updatedLevelReq
        )
        viewModel.updateMatch(updatedMatch)
        Toast.makeText(context, "Level Requirement updated to $updatedLevelReq!", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

@Composable
fun EditRulesAndRegulationDialog(
  currentRules: String,
  currentDescription: String,
  onDismiss: () -> Unit,
  onSave: (rules: String, description: String) -> Unit
) {
  var rules by remember { mutableStateOf(currentRules) }
  var description by remember { mutableStateOf(currentDescription) }

  val esportsPreset = "1. Free Fire MAX official esports settings.\n2. Room ID & Password shared 15 mins before match.\n3. Emulators strictly forbidden. Mobile players only.\n4. Screen recording or screenshot required for score verification."
  val classicPreset = "1. Classic Battle Royale tournament format.\n2. No teaming, hacking, or exploiting glitches.\n3. Room credentials posted 10 mins before start.\n4. Disputes resolved by tournament admin."
  val clashPreset = "1. Clash Squad 4v4 format.\n2. Gun attributes disabled.\n3. Emote and grenade restrictions strictly enforced.\n4. Disconnections handled per referee decisions."

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Gavel,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Edit Rules & Regulations",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Quick Presets:",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AssistChip(
            onClick = { rules = esportsPreset },
            label = { Text("Esports", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
          AssistChip(
            onClick = { rules = classicPreset },
            label = { Text("Classic BR", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
          AssistChip(
            onClick = { rules = clashPreset },
            label = { Text("Clash Squad", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = rules,
          onValueChange = { rules = it },
          label = { Text("Tournament Rules & Policy") },
          minLines = 4,
          maxLines = 8,
          modifier = Modifier.fillMaxWidth().testTag("admin_rules_input")
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("About this Match / Description") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("admin_description_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(rules.trim(), description.trim())
        },
        enabled = rules.isNotBlank()
      ) {
        Text("Save Rules")
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
fun EditPrizeDistributionDialog(
  currentPrizePool: Int,
  currentPerKill: Int,
  isDailyMatch: Boolean,
  currentDistribution: List<PrizeBreakdown>,
  onDismiss: () -> Unit,
  onSave: (pool: Int, perKill: Int, distribution: List<PrizeBreakdown>) -> Unit
) {
  var poolStr by remember { mutableStateOf(currentPrizePool.toString()) }
  val nonKillDist = currentDistribution.filterNot { it.rank.contains("Per Kill", ignoreCase = true) }
  val initialP1 = nonKillDist.find { it.rank.contains("1st", ignoreCase = true) || it.rank.contains("1", ignoreCase = true) }?.amount
    ?: nonKillDist.getOrNull(0)?.amount
    ?: (currentPrizePool * 60 / 100)
  val initialP2 = nonKillDist.find { it.rank.contains("2nd", ignoreCase = true) || it.rank.contains("2", ignoreCase = true) }?.amount
    ?: nonKillDist.getOrNull(1)?.amount
    ?: (currentPrizePool * 25 / 100)
  val initialP3 = nonKillDist.find { it.rank.contains("3rd", ignoreCase = true) || it.rank.contains("3", ignoreCase = true) }?.amount
    ?: nonKillDist.getOrNull(2)?.amount
    ?: (currentPrizePool * 15 / 100)

  var p1Str by remember { mutableStateOf(initialP1.toString()) }
  var p2Str by remember { mutableStateOf(initialP2.toString()) }
  var p3Str by remember { mutableStateOf(initialP3.toString()) }
  var perKillStr by remember { mutableStateOf(currentPerKill.takeIf { it > 0 }?.toString() ?: "25") }

  var extraRanks by remember {
    mutableStateOf(
      if (nonKillDist.size > 3) {
        nonKillDist.drop(3)
      } else {
        emptyList()
      }
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.EmojiEvents,
        contentDescription = null,
        tint = TrophyGold,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Edit Prize Distribution",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = poolStr,
          onValueChange = { poolStr = it },
          label = { Text("Total Prize Pool (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("admin_prize_pool_input")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AssistChip(
            onClick = {
              val pool = poolStr.toIntOrNull() ?: 1000
              p1Str = (pool * 60 / 100).toString()
              p2Str = (pool * 25 / 100).toString()
              p3Str = (pool * 15 / 100).toString()
            },
            label = { Text("60/25/15%", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
          AssistChip(
            onClick = {
              val pool = poolStr.toIntOrNull() ?: 1000
              p1Str = (pool * 70 / 100).toString()
              p2Str = (pool * 20 / 100).toString()
              p3Str = (pool * 10 / 100).toString()
            },
            label = { Text("70/20/10%", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
          AssistChip(
            onClick = {
              val p1 = p1Str.toIntOrNull() ?: 0
              val p2 = p2Str.toIntOrNull() ?: 0
              val p3 = p3Str.toIntOrNull() ?: 0
              val extraSum = extraRanks.sumOf { it.amount }
              poolStr = (p1 + p2 + p3 + extraSum).toString()
            },
            label = { Text("Sum to Pool", fontSize = 11.sp) },
            modifier = Modifier.weight(1f)
          )
        }

        Text(
          text = "Rank Distribution:",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = p1Str,
            onValueChange = { p1Str = it },
            label = { Text("1st (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("admin_p1_input")
          )
          OutlinedTextField(
            value = p2Str,
            onValueChange = { p2Str = it },
            label = { Text("2nd (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("admin_p2_input")
          )
          OutlinedTextField(
            value = p3Str,
            onValueChange = { p3Str = it },
            label = { Text("3rd (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("admin_p3_input")
          )
        }

        // Daily Match Kill Bonus
        if (isDailyMatch) {
          OutlinedTextField(
            value = perKillStr,
            onValueChange = { perKillStr = it },
            label = { Text("Daily Match Kill Bonus (₹ per kill)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_kill_bonus_input")
          )
        }

        // Additional Ranks list
        if (extraRanks.isNotEmpty()) {
          Text(
            text = "Additional Ranks:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
          )
          extraRanks.forEachIndexed { index, rankItem ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = rankItem.rank,
                onValueChange = { newRank ->
                  extraRanks = extraRanks.toMutableList().also { it[index] = rankItem.copy(rank = newRank) }
                },
                label = { Text("Rank") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = rankItem.amount.toString(),
                onValueChange = { newAmt ->
                  val amt = newAmt.toIntOrNull() ?: 0
                  extraRanks = extraRanks.toMutableList().also { it[index] = rankItem.copy(amount = amt) }
                },
                label = { Text("Prize (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = {
                  extraRanks = extraRanks.toMutableList().also { it.removeAt(index) }
                }
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove Rank", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        TextButton(
          onClick = {
            val nextRankNum = 4 + extraRanks.size
            extraRanks = extraRanks + PrizeBreakdown("${nextRankNum}th", 100)
          },
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Extra Rank")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val p1 = p1Str.toIntOrNull() ?: 0
          val p2 = p2Str.toIntOrNull() ?: 0
          val p3 = p3Str.toIntOrNull() ?: 0
          val killB = if (isDailyMatch) (perKillStr.toIntOrNull() ?: 0) else 0
          val pool = poolStr.toIntOrNull() ?: (p1 + p2 + p3 + extraRanks.sumOf { it.amount })

          val newDist = mutableListOf<PrizeBreakdown>()
          if (p1 > 0) newDist.add(PrizeBreakdown("1st", p1))
          if (p2 > 0) newDist.add(PrizeBreakdown("2nd", p2))
          if (p3 > 0) newDist.add(PrizeBreakdown("3rd", p3))
          extraRanks.forEach { extra ->
            if (extra.amount > 0) newDist.add(extra)
          }
          if (isDailyMatch && killB > 0) {
            newDist.add(PrizeBreakdown("Per Kill", killB))
          }
          onSave(pool, killB, newDist)
        }
      ) {
        Text("Save Prizes")
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
fun EditLevelRequirementDialog(
  currentLevelReq: String,
  onDismiss: () -> Unit,
  onSave: (newLevelReq: String) -> Unit
) {
  var levelReq by remember { mutableStateOf(currentLevelReq) }
  val quickOptions = listOf("Min LV 51", "Level 40+", "Level 50+", "Level 60+", "All Levels")

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.MilitaryTech,
        contentDescription = null,
        tint = NeonCyan,
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        text = "Edit Level Requirement",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Select a preset or enter a custom minimum player level requirement for this tournament match.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "Quick Presets:",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quickOptions.take(3).forEach { option ->
              FilterChip(
                selected = levelReq.equals(option, ignoreCase = true),
                onClick = { levelReq = option },
                label = { Text(option, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            quickOptions.drop(3).forEach { option ->
              FilterChip(
                selected = levelReq.equals(option, ignoreCase = true),
                onClick = { levelReq = option },
                label = { Text(option, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        OutlinedTextField(
          value = levelReq,
          onValueChange = { levelReq = it },
          label = { Text("Level Requirement") },
          placeholder = { Text("e.g. Min LV 51 or Level 50+") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("admin_level_req_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(levelReq.trim().ifBlank { "All Levels" })
        },
        enabled = levelReq.isNotBlank()
      ) {
        Text("Save Requirement")
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
fun InfoChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun RoomInfoBox(
  label: String,
  value: String,
  onCopy: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    modifier = modifier.clickable { onCopy() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = value,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }
      Icon(
        imageVector = Icons.Default.ContentCopy,
        contentDescription = "Copy $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}
