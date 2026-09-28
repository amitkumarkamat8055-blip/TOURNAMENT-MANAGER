package com.example.ui.custom

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CustomProfile
import com.example.data.model.CustomProfileApplication
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrophyGold
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

private fun openMediaInSystem(context: Context, mediaUrl: String, isVideo: Boolean) {
    try {
        val uri = Uri.parse(mediaUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (isVideo) {
                setDataAndType(uri, "video/*")
            } else {
                setDataAndType(uri, "image/*")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (err: Exception) {
            Toast.makeText(context, "Unable to open media: ${err.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCandidatesInspectionDialog(
    profile: CustomProfile,
    applications: List<CustomProfileApplication>,
    onDismiss: () -> Unit,
    onUpdateStatus: (appId: String, profileId: String, status: String) -> Unit,
    onDelete: (appId: String) -> Unit,
    onSendCredentials: (appId: String, roomId: String, roomPass: String) -> Unit,
    onUpdateHostPayment: ((appId: String, paid: Boolean) -> Unit)? = null,
    onUpdateCandidatePayment: ((appId: String, profileId: String, status: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var appForSendCredentials by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var candidateToDelete by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var candidateToReject by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0 = All, 1 = Accepted, 2 = Pending

    val hostUidToDisplay = profile.uid.ifBlank { profile.hostUid }
    val hostPaidAny = applications.any { it.hostPaid }

    // Identify which candidate the hoster accepted
    val acceptedCandidate = applications.firstOrNull {
        it.status.equals("Accepted", ignoreCase = true) ||
        it.status.equals("Paid", ignoreCase = true) ||
        it.status.equals("Result Submitted", ignoreCase = true) ||
        it.status.startsWith("Prize Sent", ignoreCase = true)
    }

    var hostActualName by remember(profile) { mutableStateOf(profile.hostActualName.ifBlank { profile.name }) }
    var hostPhoneOrEmail by remember(profile) { mutableStateOf(profile.hostPhone.ifBlank { profile.hostEmail }) }
    var hostGameUid by remember(profile) { mutableStateOf(profile.hostGameUid.ifBlank { profile.uid }) }

    LaunchedEffect(profile.id, profile.hostUid, profile.uid) {
        val targetUid = profile.hostUid.ifBlank { profile.uid }
        if (targetUid.isNotBlank()) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = try {
                    db.collection("users").document(targetUid).get().await()
                } catch (_: Exception) { null }
                
                if (doc != null && doc.exists()) {
                    val nameFromDoc = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    val phoneFromDoc = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
                    val emailFromDoc = doc.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                    val gameUidFromDoc = doc.getString("gameUid") ?: doc.getString("uid") ?: ""

                    if (nameFromDoc.isNotBlank()) hostActualName = nameFromDoc
                    if (phoneFromDoc.isNotBlank()) hostPhoneOrEmail = phoneFromDoc
                    else if (emailFromDoc.isNotBlank()) hostPhoneOrEmail = emailFromDoc
                    if (gameUidFromDoc.isNotBlank()) hostGameUid = gameUidFromDoc
                } else {
                    val querySnap = try {
                        db.collection("users").whereEqualTo("gameUid", profile.uid).limit(1).get().await()
                    } catch (_: Exception) { null }
                    if (querySnap != null && !querySnap.isEmpty) {
                        val uDoc = querySnap.documents[0]
                        val nameFromDoc = uDoc.getString("fullName") ?: uDoc.getString("name") ?: ""
                        val phoneFromDoc = uDoc.getString("phoneNumber") ?: uDoc.getString("phone") ?: ""
                        val emailFromDoc = uDoc.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                        if (nameFromDoc.isNotBlank()) hostActualName = nameFromDoc
                        if (phoneFromDoc.isNotBlank()) hostPhoneOrEmail = phoneFromDoc
                        else if (emailFromDoc.isNotBlank()) hostPhoneOrEmail = emailFromDoc
                    }
                }
            } catch (e: Exception) {
                Log.w("HostLookup", "Error fetching host details: ${e.message}")
            }
        }
    }

    val filteredApplications = when (selectedFilterIndex) {
        1 -> applications.filter { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Paid", ignoreCase = true) }
        2 -> applications.filter { it.status.equals("Pending", ignoreCase = true) }
        else -> applications
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ADMIN INSPECTION & PAYMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "ROOM CANDIDATES",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Manage host and candidate requests, credentials & payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Host Information & Room Card (Prominently displayed)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "HOST: $hostActualName",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hostPaidAny) SuccessGreen.copy(alpha = 0.2f) else TrophyGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (hostPaidAny) "HOST PAID: YES" else "HOST PAYMENT: PENDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (hostPaidAny) SuccessGreen else TrophyGold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (hostPhoneOrEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (hostPhoneOrEmail.contains("@")) Icons.Default.Email else Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = hostPhoneOrEmail,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Host UID: $hostGameUid",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("Host UID", hostGameUid))
                                Toast.makeText(context, "Host UID copied: $hostGameUid", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Host UID",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (profile.level.isNotBlank()) {
                            Text(
                                text = "• Level: ${profile.level}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Match: ${profile.game} • ${profile.category} • ${profile.type} • Mode: ${profile.mode} • Gun: ${profile.gun.ifBlank { "All Guns" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule: ${profile.day} at ${profile.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Payout: ₹${profile.payout}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen
                        )
                    }

                    // Admin Host Payment Action (Toggle)
                    if (onUpdateHostPayment != null && applications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val targetApp = acceptedCandidate ?: applications.first()
                                    val newStatus = !targetApp.hostPaid
                                    onUpdateHostPayment(targetApp.id, newStatus)
                                    Toast.makeText(
                                        context,
                                        if (newStatus) "Host payment marked as Received (Paid)" else "Host payment marked as Pending",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (hostPaidAny) SuccessGreen else TrophyGold
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (hostPaidAny) Icons.Default.CheckCircle else Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hostPaidAny) "Mark Host Unpaid" else "Mark Host Paid (₹${profile.payout})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Hoster Decision / Accepted Candidate Highlight
            if (acceptedCandidate != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "HOSTER ACCEPTED CANDIDATE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = acceptedCandidate.candidateName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Game UID: ${acceptedCandidate.uid} • LV: ${acceptedCandidate.level}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = acceptedCandidate.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (!acceptedCandidate.roomId.isNullOrBlank() && !acceptedCandidate.roomPassword.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Text(
                                        "Room ID: ${acceptedCandidate.roomId}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Pass: ${acceptedCandidate.roomPassword}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TrophyGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "HOSTER DECISION: PENDING",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TrophyGold
                            )
                            Text(
                                text = "Hoster has not accepted any candidate's request yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Filter Chips (All, Accepted, Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterIndex == 0,
                    onClick = { selectedFilterIndex = 0 },
                    label = { Text("All Candidates (${applications.size})") }
                )
                FilterChip(
                    selected = selectedFilterIndex == 1,
                    onClick = { selectedFilterIndex = 1 },
                    label = { Text("Accepted (${applications.count { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Paid", ignoreCase = true) }})") }
                )
                FilterChip(
                    selected = selectedFilterIndex == 2,
                    onClick = { selectedFilterIndex = 2 },
                    label = { Text("Pending (${applications.count { it.status.equals("Pending", ignoreCase = true) }})") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Candidate Requests List
            if (filteredApplications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No candidate requests found for this filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredApplications, key = { _, app -> app.id }) { index, app ->
                        val isAcceptedByHoster = app.id == acceptedCandidate?.id
                        val isRejected = app.status.equals("Rejected", ignoreCase = true)
                        val isPaid = app.status.equals("Paid", ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isAcceptedByHoster) SuccessGreen.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Match Association Banner: Host vs Candidate
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Host: $hostActualName (UID: $hostGameUid)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "⚔️ VS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isAcceptedByHoster) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isAcceptedByHoster) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = app.candidateName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Candidate UID: ${app.uid}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Candidate UID", app.uid))
                                                        Toast.makeText(context, "Candidate UID copied: ${app.uid}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ContentCopy,
                                                        contentDescription = "Copy UID",
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    text = "• LV: ${app.level}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (app.rank.isNotBlank()) {
                                                    Text(
                                                        text = "• Rank: ${app.rank}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Phone number with copy and call dialer
                                            if (app.phone.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Call,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = app.phone,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Candidate Phone", app.phone))
                                                            Toast.makeText(context, "Phone copied: ${app.phone}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Phone", modifier = Modifier.size(11.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            try {
                                                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${app.phone}"))
                                                                context.startActivity(callIntent)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.Call, contentDescription = "Call Candidate", modifier = Modifier.size(11.dp), tint = SuccessGreen)
                                                    }
                                                }
                                            }

                                            if (app.appliedAt > 0L) {
                                                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                                Text(
                                                    text = "Applied: ${sdf.format(Date(app.appliedAt))}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            isAcceptedByHoster || isPaid -> SuccessGreen.copy(alpha = 0.18f)
                                            isRejected -> AlertRed.copy(alpha = 0.18f)
                                            else -> TrophyGold.copy(alpha = 0.18f)
                                        }
                                    ) {
                                        Text(
                                            text = if (isAcceptedByHoster) "ACCEPTED" else app.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isAcceptedByHoster || isPaid -> SuccessGreen
                                                    isRejected -> AlertRed
                                                    else -> TrophyGold
                                                }
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Payment Controls Bar (Host & Candidate Payment)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Host Paid: ${if (app.hostPaid) "✓ Verified" else "Pending"}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (app.hostPaid) SuccessGreen else TrophyGold
                                                )
                                            )
                                            Text(
                                                text = "Candidate Fee: ${if (isPaid) "✓ Paid" else "Pending (${app.status})"}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPaid) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (onUpdateHostPayment != null) {
                                                OutlinedButton(
                                                    onClick = {
                                                        onUpdateHostPayment(app.id, !app.hostPaid)
                                                        Toast.makeText(context, if (!app.hostPaid) "Host payment verified!" else "Host payment set pending", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text(if (app.hostPaid) "Unmark Host" else "Verify Host", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                                }
                                            }

                                            if (onUpdateCandidatePayment != null && !isPaid) {
                                                Button(
                                                    onClick = {
                                                        onUpdateCandidatePayment(app.id, profile.id, "Paid")
                                                        Toast.makeText(context, "Candidate payment marked as Paid!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Mark Paid ✓", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Admin Action Buttons for this applicant
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (app.status == "Pending") {
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateStatus(app.id, profile.id, "Accepted")
                                                Toast.makeText(context, "Candidate ${app.candidateName} accepted!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Accept", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                        }

                                        OutlinedButton(
                                            onClick = { candidateToReject = app },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = AlertRed)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject", style = MaterialTheme.typography.labelSmall, color = AlertRed)
                                        }
                                    }

                                    Button(
                                        onClick = { appForSendCredentials = app },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send Room ID", style = MaterialTheme.typography.labelSmall)
                                    }

                                    IconButton(
                                        onClick = { candidateToDelete = app },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Application",
                                            tint = AlertRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
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

    // Send Credentials Dialog
    if (appForSendCredentials != null) {
        var roomIdInput by remember { mutableStateOf(appForSendCredentials?.roomId ?: "") }
        var roomPassInput by remember { mutableStateOf(appForSendCredentials?.roomPassword ?: "") }

        AlertDialog(
            onDismissRequest = { appForSendCredentials = null },
            title = { Text("Send Room Details to Candidate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Candidate: ${appForSendCredentials?.candidateName} (UID: ${appForSendCredentials?.uid})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Room ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                            onSendCredentials(appForSendCredentials!!.id, roomIdInput.trim(), roomPassInput.trim())
                            Toast.makeText(context, "Room credentials sent to ${appForSendCredentials?.candidateName}!", Toast.LENGTH_SHORT).show()
                            appForSendCredentials = null
                        } else {
                            Toast.makeText(context, "Please fill both Room ID & Password", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Send Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { appForSendCredentials = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Candidate Confirmation
    if (candidateToReject != null) {
        AlertDialog(
            onDismissRequest = { candidateToReject = null },
            title = { Text("Reject Candidate Request?") },
            text = { Text("Are you sure you want to reject ${candidateToReject?.candidateName}'s application?") },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(candidateToReject!!.id, profile.id, "Rejected")
                        Toast.makeText(context, "Application rejected", Toast.LENGTH_SHORT).show()
                        candidateToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { candidateToReject = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Candidate Confirmation
    if (candidateToDelete != null) {
        AlertDialog(
            onDismissRequest = { candidateToDelete = null },
            title = { Text("Delete Application?") },
            text = { Text("Remove ${candidateToDelete?.candidateName}'s application from this room?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(candidateToDelete!!.id)
                        Toast.makeText(context, "Application removed", Toast.LENGTH_SHORT).show()
                        candidateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { candidateToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResultsAndReportsDialog(
    profile: CustomProfile,
    applications: List<CustomProfileApplication>,
    onDismiss: () -> Unit,
    onSendWinningAmount: (app: CustomProfileApplication, winnerUid: String, amount: Int) -> Unit,
    onDismissReport: (app: CustomProfileApplication) -> Unit,
    onRefundOrCredit: ((targetUid: String, amount: Int, reason: String) -> Unit)? = null,
    onUpdateHostPayment: ((appId: String, paid: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }
    var selectedAppForPayout by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Result Photos, 1 = Reports

    val hostUidToDisplay = profile.uid.ifBlank { profile.hostUid }
    val resultApps = applications.filter { it.isResultSubmitted || it.resultScreenshot.isNotBlank() }
    val reportedApps = applications.filter { it.isReported || it.reportReason.isNotBlank() || it.reportDescription.isNotBlank() }

    var hostActualName by remember(profile) { mutableStateOf(profile.hostActualName.ifBlank { profile.name }) }
    var hostPhoneOrEmail by remember(profile) { mutableStateOf(profile.hostPhone.ifBlank { profile.hostEmail }) }
    var hostGameUid by remember(profile) { mutableStateOf(profile.hostGameUid.ifBlank { profile.uid }) }

    LaunchedEffect(profile.id, profile.hostUid, profile.uid) {
        val targetUid = profile.hostUid.ifBlank { profile.uid }
        if (targetUid.isNotBlank()) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = try {
                    db.collection("users").document(targetUid).get().await()
                } catch (_: Exception) { null }
                
                if (doc != null && doc.exists()) {
                    val nameFromDoc = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    val phoneFromDoc = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
                    val emailFromDoc = doc.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                    val gameUidFromDoc = doc.getString("gameUid") ?: doc.getString("uid") ?: ""

                    if (nameFromDoc.isNotBlank()) hostActualName = nameFromDoc
                    if (phoneFromDoc.isNotBlank()) hostPhoneOrEmail = phoneFromDoc
                    else if (emailFromDoc.isNotBlank()) hostPhoneOrEmail = emailFromDoc
                    if (gameUidFromDoc.isNotBlank()) hostGameUid = gameUidFromDoc
                } else {
                    val querySnap = try {
                        db.collection("users").whereEqualTo("gameUid", profile.uid).limit(1).get().await()
                    } catch (_: Exception) { null }
                    if (querySnap != null && !querySnap.isEmpty) {
                        val uDoc = querySnap.documents[0]
                        val nameFromDoc = uDoc.getString("fullName") ?: uDoc.getString("name") ?: ""
                        val phoneFromDoc = uDoc.getString("phoneNumber") ?: uDoc.getString("phone") ?: ""
                        val emailFromDoc = uDoc.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                        if (nameFromDoc.isNotBlank()) hostActualName = nameFromDoc
                        if (phoneFromDoc.isNotBlank()) hostPhoneOrEmail = phoneFromDoc
                        else if (emailFromDoc.isNotBlank()) hostPhoneOrEmail = emailFromDoc
                    }
                }
            } catch (e: Exception) {
                Log.w("HostLookup", "Error fetching host details: ${e.message}")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ADMIN VERIFICATION & PAYMENTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "RESULTS & DISPUTES",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Match result screenshots, video reports & payment settlements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Room & Host Summary Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Host: $hostActualName",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hostGameUid.isNotBlank()) {
                                Text(
                                    text = "(UID: $hostGameUid)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Host UID", hostGameUid))
                                        Toast.makeText(context, "Host UID copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (hostPhoneOrEmail.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (hostPhoneOrEmail.contains("@")) Icons.Default.Email else Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = hostPhoneOrEmail,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${profile.game} • ${profile.type} • Mode: ${profile.mode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Prize Payout",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${profile.payout}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Result Photos (${resultApps.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Dispute Reports (${reportedApps.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content Area
            if (selectedTab == 0) {
                // Result Photos Tab
                if (resultApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Match Result Photos Submitted Yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Once the match concludes, the host and candidate upload their Booyah screenshot proof and declared winner details here for admin verification and prize payout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(resultApps, key = { _, item -> item.id }) { index, app ->
                            val payoutAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
                            val winnerUid = app.winnerUid.ifBlank { app.uid }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = TrophyGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Match Result #${index + 1}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (app.winningAmountSent) SuccessGreen.copy(alpha = 0.2f) else TrophyGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (app.winningAmountSent) "PRIZE PAID" else "AWAITING PAYOUT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (app.winningAmountSent) SuccessGreen else TrophyGold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Match Host & Candidate Information Card
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    text = "Room Host: $hostActualName (UID: $hostGameUid)",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (app.resultSubmittedBy.isNotBlank()) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                                        Text(
                                                            text = "By: ${app.resultSubmittedBy}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (hostPhoneOrEmail.isNotBlank()) {
                                                Text(
                                                    text = "Host Contact: $hostPhoneOrEmail",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = "Candidate: ${app.candidateName} (UID: ${app.uid})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Declared Winner:",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${app.winnerName.ifBlank { app.candidateName }} (UID: $winnerUid)",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = SuccessGreen
                                                )
                                                IconButton(
                                                    onClick = {
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Winner UID", winnerUid))
                                                        Toast.makeText(context, "Winner UID copied: $winnerUid", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Winner UID", modifier = Modifier.size(12.dp), tint = SuccessGreen)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Match Result Screenshot Photo
                                    if (app.resultScreenshot.isNotBlank()) {
                                        Text(
                                            text = "Match Result Photo (Click to zoom):",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { selectedImageForPreview = app.resultScreenshot }
                                        ) {
                                            AsyncImage(
                                                model = app.resultScreenshot,
                                                contentDescription = "Match Result Screenshot",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Text("Tap to Zoom", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Payout Management Section (Admin Only)
                                    if (app.winningAmountSent) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Prize of ₹$payoutAmount already credited to winner UID: ${app.winningAmountSentTo.ifBlank { winnerUid }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { selectedAppForPayout = app },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send Prize Money (₹$payoutAmount) to Winner")
                                            }

                                            // Direct Payout Actions (Admin can pay winner, host, or refund candidate)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (onRefundOrCredit != null) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            onRefundOrCredit(hostUidToDisplay, payoutAmount, "Host Payout for Custom Room")
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Pay Host (₹$payoutAmount)", style = MaterialTheme.typography.labelSmall)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            onRefundOrCredit(app.uid, payoutAmount, "Candidate Refund / Prize")
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Refund Cand. (₹$payoutAmount)", style = MaterialTheme.typography.labelSmall)
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
            } else {
                // Reports Tab (Photos & Videos)
                if (reportedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen.copy(alpha = 0.6f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Dispute Reports Filed",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No dispute reports or rule violations with photo/video proof have been submitted by the candidate or hoster for this room.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(reportedApps, key = { _, item -> item.id }) { index, app ->
                            val payoutAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
                            val mediaUrl = app.reportMediaUrl
                            val isVideo = mediaUrl.contains(".mp4", ignoreCase = true) ||
                                          mediaUrl.contains(".mov", ignoreCase = true) ||
                                          mediaUrl.contains(".mkv", ignoreCase = true) ||
                                          mediaUrl.contains(".webm", ignoreCase = true) ||
                                          mediaUrl.contains("video", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = AlertRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Dispute Report #${index + 1}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = AlertRed
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AlertRed.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = if (app.reportSubmittedBy.isNotBlank()) "BY: ${app.reportSubmittedBy.uppercase()}" else "REPORTED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = AlertRed
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Match Host & Candidate Info
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = "Host: $hostActualName (UID: $hostGameUid)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hostPhoneOrEmail.isNotBlank()) {
                                                Text(
                                                    text = "Host Contact: $hostPhoneOrEmail",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Candidate: ${app.candidateName} (UID: ${app.uid})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (app.phone.isNotBlank()) {
                                                Text(
                                                    text = "Candidate Phone: ${app.phone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (app.reportReason.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AlertRed.copy(alpha = 0.1f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Reason: ${app.reportReason}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = AlertRed,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    if (app.reportDescription.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Description: ${app.reportDescription}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Evidence Media: Photo & Video Support
                                    if (mediaUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isVideo) "Evidence Video Proof:" else "Evidence Photo Screenshot:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isVideo) MaterialTheme.colorScheme.primaryContainer else TrophyGold.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (isVideo) "VIDEO" else "IMAGE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isVideo) MaterialTheme.colorScheme.onPrimaryContainer else TrophyGold,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (isVideo) {
                                            // Video Evidence Card with Watch Action
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.Black.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { openMediaInSystem(context, mediaUrl, true) }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircle,
                                                        contentDescription = "Play Video",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Video Evidence Submitted",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "Tap to open and play match video recording",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Button(
                                                        onClick = { openMediaInSystem(context, mediaUrl, true) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Watch Video Evidence")
                                                    }
                                                }
                                            }
                                        } else {
                                            // Photo Evidence Card with Zoom
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { selectedImageForPreview = mediaUrl }
                                            ) {
                                                AsyncImage(
                                                    model = mediaUrl,
                                                    contentDescription = "Report Evidence",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Text("Tap to Zoom", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Admin Resolution & Payment Management Actions
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (onRefundOrCredit != null) {
                                                Button(
                                                    onClick = {
                                                        onRefundOrCredit(app.uid, payoutAmount, "Dispute Resolution Refund")
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Refund Candidate (₹$payoutAmount)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                                }

                                                Button(
                                                    onClick = {
                                                        onRefundOrCredit(hostUidToDisplay, payoutAmount, "Dispute Resolution Host Payout")
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Pay Host (₹$payoutAmount)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                onDismissReport(app)
                                                Toast.makeText(context, "Report dismissed / marked resolved", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Dismiss Report / Mark Resolved")
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

    // Full-Screen Image Preview Dialog
    if (selectedImageForPreview != null) {
        Dialog(onDismissRequest = { selectedImageForPreview = null }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.8f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proof Screenshot Preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { selectedImageForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedImageForPreview,
                            contentDescription = "Proof Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Payout Confirmation Dialog
    if (selectedAppForPayout != null) {
        val app = selectedAppForPayout!!
        val defaultAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
        var amountInput by remember { mutableStateOf(defaultAmount.toString()) }
        var winnerUidInput by remember { mutableStateOf(app.winnerUid.ifBlank { app.uid }) }

        AlertDialog(
            onDismissRequest = { selectedAppForPayout = null },
            title = { Text("Send Prize Money to Winner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Host: $hostActualName (UID: $hostGameUid)" + if (hostPhoneOrEmail.isNotBlank()) " | Contact: $hostPhoneOrEmail" else "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Candidate: ${app.candidateName} (UID: ${app.uid})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = winnerUidInput,
                        onValueChange = { winnerUidInput = it },
                        label = { Text("Winner Free Fire UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Winning Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toIntOrNull() ?: defaultAmount
                        onSendWinningAmount(app, winnerUidInput.trim(), amount)
                        Toast.makeText(context, "₹$amount sent to winner (UID: $winnerUidInput)!", Toast.LENGTH_SHORT).show()
                        selectedAppForPayout = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForPayout = null }) { Text("Cancel") }
            }
        )
    }
}
