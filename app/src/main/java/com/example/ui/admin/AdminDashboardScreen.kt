package com.example.ui.admin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CustomProfileApplication
import com.example.data.model.MatchItem
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.ui.viewmodel.TournamentViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private val AdminPrimary = Color(0xFF1E88E5)
private val AdminSuccess = Color(0xFF2E7D32)
private val AdminAlert = Color(0xFFC62828)
private val AdminGold = Color(0xFFD97706)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: TournamentViewModel,
    modifier: Modifier = Modifier
) {
    var selectedAdminTab by remember { mutableIntStateOf(0) }
    val adminTabs = listOf("Matches", "Candidates", "Custom Rooms & Payouts", "Users")

    val matches by viewModel.allMatches.collectAsStateWithLifecycle()
    val matchRegistrations by viewModel.allMatchRegistrations.collectAsStateWithLifecycle()
    val customApplications by viewModel.allCustomProfileApplicationsForAdmin.collectAsStateWithLifecycle()
    val users by viewModel.adminUsersList.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.fetchAdminUsers()
        viewModel.fetchAllCustomProfileApplicationsForAdmin()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Admin Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Management Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tournaments, Candidates & Payouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedAdminTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    adminTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedAdminTab == index,
                            onClick = { selectedAdminTab = index },
                            text = {
                                val badgeCount = when (index) {
                                    0 -> matches.size
                                    1 -> matchRegistrations.size
                                    2 -> customApplications.count { it.isResultSubmitted || it.isReported }
                                    3 -> users.size
                                    else -> 0
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedAdminTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (badgeCount > 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = if (selectedAdminTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "$badgeCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selectedAdminTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedAdminTab) {
                0 -> AdminMatchesTab(
                    matches = matches,
                    registrations = matchRegistrations,
                    viewModel = viewModel
                )
                1 -> AdminCandidatesTab(
                    matches = matches,
                    registrations = matchRegistrations,
                    viewModel = viewModel
                )
                2 -> AdminCustomRoomsTab(
                    applications = customApplications,
                    viewModel = viewModel
                )
                3 -> AdminUsersTab(
                    users = users,
                    viewModel = viewModel
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: MATCHES (DAILY & WEEKLY MANAGEMENT)
// -------------------------------------------------------------
@Composable
fun AdminMatchesTab(
    matches: List<MatchItem>,
    registrations: List<MatchRegistration>,
    viewModel: TournamentViewModel
) {
    var matchFilter by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var matchToEdit by remember { mutableStateOf<MatchItem?>(null) }
    var matchForRoomCredentials by remember { mutableStateOf<MatchItem?>(null) }
    var matchToDelete by remember { mutableStateOf<MatchItem?>(null) }

    val filteredMatches = remember(matches, matchFilter) {
        when (matchFilter) {
            "Daily" -> matches.filter { it.name.contains("Daily", ignoreCase = true) }
            "Weekly" -> matches.filter { it.name.contains("Weekly", ignoreCase = true) }
            else -> matches
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Daily", "Weekly").forEach { category ->
                    FilterChip(
                        selected = matchFilter == category,
                        onClick = { matchFilter = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_create_match_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Match", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No matches found in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMatches, key = { it.id }) { match ->
                    val candidateCount = registrations.count { it.matchId == match.id }
                    AdminMatchCard(
                        match = match,
                        candidateCount = candidateCount,
                        onEdit = { matchToEdit = match },
                        onSendRoomId = { matchForRoomCredentials = match },
                        onDelete = { matchToDelete = match }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditMatchDialog(
            existingMatch = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, game, cat, fee, prize, maxP, date, time, format, map, rules, perKill, bannerUrl ->
                viewModel.createTournamentMatch(name, game, cat, fee, prize, maxP, date, time, format, map, rules, perKill, bannerUrl) {
                    showCreateDialog = false
                }
            }
        )
    }

    if (matchToEdit != null) {
        CreateOrEditMatchDialog(
            existingMatch = matchToEdit,
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
                    matchToEdit!!.id,
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

    if (matchForRoomCredentials != null) {
        SendRoomIdDialog(
            match = matchForRoomCredentials!!,
            registeredCandidatesCount = registrations.count { it.matchId == matchForRoomCredentials!!.id },
            onDismiss = { matchForRoomCredentials = null },
            onBroadcast = { roomId, password ->
                viewModel.broadcastRoomCredentials(matchForRoomCredentials!!.id, roomId, password) {
                    matchForRoomCredentials = null
                }
            }
        )
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Delete Tournament Match?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${matchToDelete!!.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTournamentMatch(matchToDelete!!.id)
                        matchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminAlert)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminMatchCard(
    match: MatchItem,
    candidateCount: Int,
    onEdit: () -> Unit,
    onSendRoomId: () -> Unit,
    onDelete: () -> Unit
) {
    val isDaily = match.name.contains("Daily", ignoreCase = true)
    val isWeekly = match.name.contains("Weekly", ignoreCase = true)
    val categoryLabel = if (isDaily) "Daily Match" else if (isWeekly) "Weekly Match" else "Tournament"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_match_card_${match.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isDaily) AdminPrimary.copy(alpha = 0.15f) else AdminGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDaily) AdminPrimary else AdminGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = match.gameTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Match", tint = AdminPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Match", tint = AdminAlert.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = match.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Fee", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${match.entryFee}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Prize Pool", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${match.prizePool}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AdminGold)
                }
                Column {
                    Text("Registered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$candidateCount / ${match.maxPlayers}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Timing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${match.date} ${match.time}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (match.roomId.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = AdminSuccess, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Room: ${match.roomId} | Pass: ${match.roomPassword}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AdminSuccess
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Room ID not broadcasted yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSendRoomId,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (match.roomId.isNotBlank()) "Update Room" else "Send Room ID",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CANDIDATES MANAGEMENT (REGISTERED CANDIDATES & PAYMENT STATUS)
// -------------------------------------------------------------
@Composable
fun AdminCandidatesTab(
    matches: List<MatchItem>,
    registrations: List<MatchRegistration>,
    viewModel: TournamentViewModel
) {
    var selectedMatchId by remember { mutableLongStateOf(0L) }
    var searchQuery by remember { mutableStateOf("") }
    var candidateForRoomCredentials by remember { mutableStateOf<MatchRegistration?>(null) }
    var showBroadcastDialogForMatch by remember { mutableStateOf<MatchItem?>(null) }

    val filteredRegistrations = remember(registrations, selectedMatchId, searchQuery) {
        registrations.filter { reg ->
            val matchMatches = if (selectedMatchId == 0L) true else reg.matchId == selectedMatchId
            val queryMatches = if (searchQuery.isBlank()) true else {
                reg.candidateName.contains(searchQuery, ignoreCase = true) ||
                reg.gameUid.contains(searchQuery, ignoreCase = true) ||
                reg.phoneOrEmail.contains(searchQuery, ignoreCase = true)
            }
            matchMatches && queryMatches
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by candidate name or game UID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedMatchId == 0L,
                    onClick = { selectedMatchId = 0L },
                    label = { Text("All Matches (${registrations.size})") }
                )
            }
            items(matches, key = { it.id }) { match ->
                val count = registrations.count { it.matchId == match.id }
                FilterChip(
                    selected = selectedMatchId == match.id,
                    onClick = { selectedMatchId = match.id },
                    label = { Text("${match.name} ($count)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedMatchId != 0L) {
            val selectedMatch = matches.find { it.id == selectedMatchId }
            if (selectedMatch != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Broadcast Room ID & Password",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Send credentials to all ${filteredRegistrations.size} registered candidates for this match.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { showBroadcastDialogForMatch = selectedMatch },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Send to All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (filteredRegistrations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No registered candidates found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredRegistrations, key = { it.id }) { reg ->
                    AdminCandidateCard(
                        registration = reg,
                        onTogglePayment = {
                            val newStatus = if (reg.paymentStatus == "Paid") "Pending" else "Paid"
                            viewModel.updateRegistrationPaymentStatus(reg.id, newStatus)
                        },
                        onSendRoomId = {
                            candidateForRoomCredentials = reg
                        }
                    )
                }
            }
        }
    }

    if (showBroadcastDialogForMatch != null) {
        SendRoomIdDialog(
            match = showBroadcastDialogForMatch!!,
            registeredCandidatesCount = registrations.count { it.matchId == showBroadcastDialogForMatch!!.id },
            onDismiss = { showBroadcastDialogForMatch = null },
            onBroadcast = { roomId, password ->
                viewModel.broadcastRoomCredentials(showBroadcastDialogForMatch!!.id, roomId, password) {
                    showBroadcastDialogForMatch = null
                }
            }
        )
    }

    if (candidateForRoomCredentials != null) {
        val targetMatch = matches.find { it.id == candidateForRoomCredentials!!.matchId }
        val dummyMatch = targetMatch ?: MatchItem(
            id = candidateForRoomCredentials!!.matchId,
            matchNumber = 1,
            name = candidateForRoomCredentials!!.matchTitle,
            gameTitle = "Free Fire",
            entryFee = candidateForRoomCredentials!!.entryFee,
            prizePool = 1000,
            totalPlayers = 1,
            maxPlayers = 48,
            date = "Today",
            time = "Now",
            rankRequirement = "None",
            region = "India",
            status = MatchStatus.OPEN,
            format = "Solo",
            mapName = "Bermuda",
            rules = "Standard",
            description = "",
            prizeDistribution = emptyList(),
            roomId = candidateForRoomCredentials!!.roomId,
            roomPassword = candidateForRoomCredentials!!.roomPassword,
            isJoined = true,
            userGameUid = candidateForRoomCredentials!!.gameUid
        )

        SendRoomIdDialog(
            match = dummyMatch,
            registeredCandidatesCount = 1,
            onDismiss = { candidateForRoomCredentials = null },
            onBroadcast = { roomId, password ->
                viewModel.broadcastRoomCredentials(candidateForRoomCredentials!!.matchId, roomId, password) {
                    candidateForRoomCredentials = null
                }
            }
        )
    }
}

@Composable
fun AdminCandidateCard(
    registration: MatchRegistration,
    onTogglePayment: () -> Unit,
    onSendRoomId: () -> Unit
) {
    val isPaid = registration.paymentStatus.equals("Paid", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("admin_candidate_card_${registration.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = registration.candidateName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Game UID: ${registration.gameUid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isPaid) AdminSuccess.copy(alpha = 0.15f) else AdminAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onTogglePayment() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (isPaid) AdminSuccess else AdminAlert,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isPaid) "PAID" else "PENDING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) AdminSuccess else AdminAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Match", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(registration.matchTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column {
                    Text("Entry Fee", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${registration.entryFee}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Contact", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(registration.phoneOrEmail.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (registration.roomId.isNotBlank()) {
                        Text(
                            text = "Room: ${registration.roomId} | Pass: ${registration.roomPassword}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Room credentials not sent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        TextButton(
                            onClick = onTogglePayment,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                if (isPaid) "Mark Pending" else "Mark Paid",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick = onSendRoomId,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Send Room", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: CUSTOM ROOMS & PAYOUTS MANAGEMENT
// -------------------------------------------------------------
@Composable
fun AdminCustomRoomsTab(
    applications: List<CustomProfileApplication>,
    viewModel: TournamentViewModel
) {
    var roomFilter by remember { mutableStateOf("All") }
    var selectedAppForPayout by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var selectedScreenshotForPreview by remember { mutableStateOf<String?>(null) }

    val filteredApps = remember(applications, roomFilter) {
        when (roomFilter) {
            "Results" -> applications.filter { it.isResultSubmitted }
            "Reported" -> applications.filter { it.isReported }
            "Payouts" -> applications.filter { it.winningAmountSent }
            "Pending Review" -> applications.filter { it.isResultSubmitted && !it.winningAmountSent }
            else -> applications
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Pending Review", "Results", "Reported", "Payouts")) { filter ->
                    FilterChip(
                        selected = roomFilter == filter,
                        onClick = { roomFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No custom profile rooms match this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.id }) { app ->
                    AdminCustomRoomCard(
                        application = app,
                        onPreviewScreenshot = { selectedScreenshotForPreview = it },
                        onSendWinningAmount = { selectedAppForPayout = app },
                        onDismissReport = {
                            viewModel.submitMatchReport(app.id, "", "", "")
                        }
                    )
                }
            }
        }
    }

    if (selectedAppForPayout != null) {
        SendWinningAmountDialog(
            application = selectedAppForPayout!!,
            onDismiss = { selectedAppForPayout = null },
            onSend = { winnerUid, amount ->
                viewModel.sendCustomRoomWinningAmount(selectedAppForPayout!!, winnerUid, amount) {
                    selectedAppForPayout = null
                }
            }
        )
    }

    if (selectedScreenshotForPreview != null) {
        AlertDialog(
            onDismissRequest = { selectedScreenshotForPreview = null },
            title = { Text("Result Proof Screenshot", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = selectedScreenshotForPreview,
                        contentDescription = "Full Screenshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedScreenshotForPreview = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AdminCustomRoomCard(
    application: CustomProfileApplication,
    onPreviewScreenshot: (String) -> Unit,
    onSendWinningAmount: () -> Unit,
    onDismissReport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (application.isReported) 1.5.dp else 1.dp,
            color = if (application.isReported) AdminAlert.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("admin_custom_room_card_${application.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.profileName.ifBlank { "Custom Room Match" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val hostName = application.hostActualName.ifBlank { "Host" }
                    val hostContact = application.hostPhone.ifBlank { application.hostEmail }
                    val hostGameUid = application.hostGameUid.ifBlank { application.uid }
                    Text(
                        text = "Host: $hostName" + 
                            (if (hostContact.isNotBlank()) " | Phone: $hostContact" else "") + 
                            (if (hostGameUid.isNotBlank()) " | UID: $hostGameUid" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when {
                        application.winningAmountSent -> AdminSuccess.copy(alpha = 0.15f)
                        application.isReported -> AdminAlert.copy(alpha = 0.15f)
                        application.isResultSubmitted -> AdminGold.copy(alpha = 0.2f)
                        application.status == "Paid" -> AdminPrimary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            application.winningAmountSent -> "PRIZE PAID"
                            application.isReported -> "REPORTED ⚠️"
                            application.isResultSubmitted -> "RESULT READY"
                            else -> application.status.uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            application.winningAmountSent -> AdminSuccess
                            application.isReported -> AdminAlert
                            application.isResultSubmitted -> AdminGold
                            application.status == "Paid" -> AdminPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Candidate Player", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${application.candidateName} (UID: ${application.uid})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Player Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Lv. ${application.level}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (application.isResultSubmitted) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminGold.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, AdminGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AdminGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Match Result Submitted", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AdminGold)
                            }

                            if (application.resultScreenshot.isNotBlank()) {
                                TextButton(
                                    onClick = { onPreviewScreenshot(application.resultScreenshot) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("View Proof", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Winner: ${application.winnerName.ifBlank { application.candidateName }} (UID: ${application.winnerUid.ifBlank { application.uid }})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (application.winningAmountSent) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✅ Winning amount sent to UID ${application.winningAmountSentTo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdminSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onSendWinningAmount,
                                colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("admin_send_winning_amount_button_${application.id}")
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Verify & Send Winning Amount (₹)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (application.isReported) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdminAlert.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, AdminAlert.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AdminAlert, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Candidate Report Filed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AdminAlert)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${application.reportReason.ifBlank { "Unfair gameplay / dispute" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (application.reportDescription.isNotBlank()) {
                            Text(
                                text = "Details: ${application.reportDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onDismissReport,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Dismiss Report", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onSendWinningAmount,
                                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Award Winner", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (!application.isResultSubmitted && !application.isReported) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (application.roomId?.isNotBlank() == true) "Room ID: ${application.roomId}" else "Room not started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onSendWinningAmount,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Send Winning Amount", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: REGISTERED USERS LIST
// -------------------------------------------------------------
@Composable
fun AdminUsersTab(
    users: List<Map<String, Any>>,
    viewModel: TournamentViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Registered Players & Users (${users.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(users) { userMap ->
                    AdminUserCard(userMap)
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(userMap: Map<String, Any>) {
    val email = userMap["email"] as? String ?: "No Email"
    val name = userMap["fullName"] as? String ?: userMap["name"] as? String ?: "Unknown User"

    val createdAtTimestamp = userMap["createdAt"] as? Timestamp
    val createdAtStr = if (createdAtTimestamp != null) {
        val date = createdAtTimestamp.toDate()
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
    } else {
        "Recent"
    }

    val walletBalance = (userMap["walletBalance"] as? Number)?.toLong() ?: 0L
    val matchesJoined = (userMap["matchesJoined"] as? Number)?.toLong() ?: 0L
    val gameUid = userMap["gameUid"] as? String ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = AdminSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹$walletBalance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AdminSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (gameUid.isNotBlank()) {
                    Text(text = "Game UID: $gameUid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(text = "Joined: $createdAtStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = "Matches Joined: $matchesJoined", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------

@Composable
fun CreateOrEditMatchDialog(
    existingMatch: MatchItem?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        game: String,
        category: String,
        entryFee: Int,
        prizePool: Int,
        maxPlayers: Int,
        date: String,
        time: String,
        format: String,
        map: String,
        rules: String,
        perKill: Int,
        bannerImageUrl: String
    ) -> Unit
) {
    val isEdit = existingMatch != null

    var category by remember { mutableStateOf(if (existingMatch?.name?.contains("Weekly", ignoreCase = true) == true) "Weekly" else "Daily") }
    var matchName by remember { mutableStateOf(existingMatch?.name?.replace("Daily ", "")?.replace("Weekly ", "") ?: "Clash Squad Blitz") }
    var gameTitle by remember { mutableStateOf(existingMatch?.gameTitle ?: "Free Fire MAX") }
    var entryFeeStr by remember { mutableStateOf(existingMatch?.entryFee?.toString() ?: "30") }
    var prizePoolStr by remember { mutableStateOf(existingMatch?.prizePool?.toString() ?: "1000") }
    var perKillStr by remember { mutableStateOf(existingMatch?.perKill?.takeIf { it > 0 }?.toString() ?: "15") }
    var maxPlayersStr by remember { mutableStateOf(existingMatch?.maxPlayers?.toString() ?: "48") }
    var dateStr by remember { mutableStateOf(existingMatch?.date ?: "Today") }
    var timeStr by remember { mutableStateOf(existingMatch?.time ?: "08:00 PM") }
    var formatStr by remember {
        mutableStateOf(
            when (existingMatch?.format?.trim()?.lowercase()) {
                "solo" -> "Solo"
                "duo" -> "Duo"
                else -> "Squad"
            }
        )
    }
    var mapStr by remember { mutableStateOf(existingMatch?.mapName ?: "Bermuda") }
    var rulesStr by remember { mutableStateOf(existingMatch?.rules ?: "No Emotes, No Grenades, Classic Battle Royale rules apply.") }
    var bannerUriStr by remember { mutableStateOf(existingMatch?.bannerImageUrl ?: "") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val safeDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            bannerUriStr = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Tournament Match" else "Create Tournament Match",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Match Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("$cat Match") }
                        )
                    }
                }

                // Match Profile Banner Upload
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Match Profile Banner",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (bannerUriStr.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = bannerUriStr,
                                contentDescription = "Match Banner Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { bannerUriStr = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Banner",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_upload_banner_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (bannerUriStr.isBlank()) Icons.Default.AddPhotoAlternate else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (bannerUriStr.isBlank()) "Upload Match Banner" else "Change Banner Image")
                    }
                }

                OutlinedTextField(
                    value = matchName,
                    onValueChange = { matchName = it },
                    label = { Text("Match Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_match_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFeeStr,
                        onValueChange = { entryFeeStr = it },
                        label = { Text("Entry Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_entry_fee_input")
                    )
                    OutlinedTextField(
                        value = prizePoolStr,
                        onValueChange = { prizePoolStr = it },
                        label = { Text("Prize Pool (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("admin_prize_pool_input")
                    )
                }

                // Per Kill input: Strictly ONLY for Daily Matches
                if (category == "Daily") {
                    OutlinedTextField(
                        value = perKillStr,
                        onValueChange = { perKillStr = it },
                        label = { Text("Per Kill Bounty (₹) - Daily Only") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_per_kill_input")
                    )
                }

                // Format Selection: Options only (Solo, Duo, Squad)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Match Format",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Solo", "Duo", "Squad").forEach { opt ->
                            FilterChip(
                                selected = formatStr.equals(opt, ignoreCase = true),
                                onClick = { formatStr = opt },
                                label = {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = opt,
                                            fontWeight = if (formatStr.equals(opt, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_format_$opt")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = maxPlayersStr,
                    onValueChange = { maxPlayersStr = it },
                    label = { Text("Max Players") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_max_players_input")
                )

                // Match Day & Time Selection (Today & Tomorrow with Time)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Match Schedule",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Two options for Day: Today & Tomorrow
                        Row(
                            modifier = Modifier.weight(1.1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Today", "Tomorrow").forEach { dayOpt ->
                                FilterChip(
                                    selected = dateStr.equals(dayOpt, ignoreCase = true),
                                    onClick = { dateStr = dayOpt },
                                    label = {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = dayOpt,
                                                fontWeight = if (dateStr.equals(dayOpt, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_day_$dayOpt")
                                )
                            }
                        }

                        // Time Input
                        OutlinedTextField(
                            value = timeStr,
                            onValueChange = { timeStr = it },
                            label = { Text("Time") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("admin_time_input")
                        )
                    }
                }

                OutlinedTextField(
                    value = mapStr,
                    onValueChange = { mapStr = it },
                    label = { Text("Map Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rulesStr,
                    onValueChange = { rulesStr = it },
                    label = { Text("Rules & Guidelines") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val fee = entryFeeStr.toIntOrNull() ?: 0
                    val prize = prizePoolStr.toIntOrNull() ?: 0
                    val maxP = maxPlayersStr.toIntOrNull() ?: 48
                    val pKill = if (category == "Daily") (perKillStr.toIntOrNull() ?: 0) else 0
                    onSave(matchName, gameTitle, category, fee, prize, maxP, dateStr, timeStr, formatStr, mapStr, rulesStr, pKill, bannerUriStr)
                },
                enabled = matchName.isNotBlank()
            ) {
                Text(if (isEdit) "Update Match" else "Create Match")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SendRoomIdDialog(
    match: MatchItem,
    registeredCandidatesCount: Int,
    onDismiss: () -> Unit,
    onBroadcast: (roomId: String, password: String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val safeDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    var roomId by remember { mutableStateOf(match.roomId) }
    var password by remember { mutableStateOf(match.roomPassword) }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = {
            Text("Send Room ID & Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = match.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sending credentials to $registeredCandidatesCount registered candidate(s).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("Custom Room ID") },
                    placeholder = { Text("e.g. 5829104") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_room_id_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Room Password") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_room_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onBroadcast(roomId, password)
                },
                enabled = roomId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess),
                modifier = Modifier.testTag("admin_broadcast_room_confirm")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Broadcast to All")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SendWinningAmountDialog(
    application: CustomProfileApplication,
    onDismiss: () -> Unit,
    onSend: (winnerUid: String, amount: Int) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val safeDismiss = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    val initialWinnerUid = application.winnerUid.ifBlank { application.uid }
    val initialAmount = application.payout.replace("[^0-9]".toRegex(), "").ifBlank { "500" }

    var winnerUid by remember { mutableStateOf(initialWinnerUid) }
    var winnerName by remember { mutableStateOf(application.winnerName.ifBlank { application.candidateName }) }
    var amountStr by remember { mutableStateOf(initialAmount) }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = {
            Text("Send Winning Amount", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = AdminGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Room: ${application.profileName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AdminGold
                        )
                        Text(
                            text = "Disburse prize pool to the verified winner's in-app wallet balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = winnerName,
                    onValueChange = { winnerName = it },
                    label = { Text("Winner Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = winnerUid,
                    onValueChange = { winnerUid = it },
                    label = { Text("Winner Game UID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_winner_uid_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Winning Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_winning_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val amount = amountStr.toIntOrNull() ?: 0
                    onSend(winnerUid, amount)
                },
                enabled = winnerUid.isNotBlank() && (amountStr.toIntOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AdminSuccess),
                modifier = Modifier.testTag("admin_confirm_send_winning_amount")
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirm & Disburse ₹$amountStr")
            }
        },
        dismissButton = {
            TextButton(onClick = safeDismiss) { Text("Cancel") }
        }
    )
}
