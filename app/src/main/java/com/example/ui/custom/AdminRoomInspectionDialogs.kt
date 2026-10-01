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
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0 = All Candidates, 1 = Accepted
    val candidateContactMap = remember { mutableStateMapOf<String, Pair<String, String>>() }

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

    LaunchedEffect(applications) {
        val uids = applications.flatMap { listOf(it.applicantUid, it.uid) }.filter { it.isNotBlank() }.distinct()
        if (uids.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            uids.forEach { uId ->
                if (!candidateContactMap.containsKey(uId)) {
                    try {
                        val d = db.collection("users").document(uId).get().await()
                        if (d.exists()) {
                            val em = d.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                            val ph = d.getString("phoneNumber") ?: d.getString("phone") ?: ""
                            candidateContactMap[uId] = Pair(em, ph)
                        } else {
                            val q = db.collection("users").whereEqualTo("gameUid", uId).limit(1).get().await()
                            if (!q.isEmpty) {
                                val doc = q.documents[0]
                                val em = doc.getString("email")?.takeIf { !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
                                val ph = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
                                candidateContactMap[uId] = Pair(em, ph)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val filteredApplications = when (selectedFilterIndex) {
        1 -> applications.filter { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Paid", ignoreCase = true) }
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

            // 3. Filter Chips (All Candidates, Accepted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterIndex == 0,
                    onClick = { selectedFilterIndex = 0 },
                    label = { Text("All Candidates (${applications.size})") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilterIndex == 1,
                    onClick = { selectedFilterIndex = 1 },
                    label = { Text("Accepted (${applications.count { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Paid", ignoreCase = true) }})") },
                    modifier = Modifier.weight(1f)
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

                                            val candEmail = app.email.ifBlank {
                                                if (app.phone.contains("@")) app.phone
                                                else candidateContactMap[app.applicantUid]?.first?.takeIf { it.isNotBlank() }
                                                    ?: candidateContactMap[app.uid]?.first?.takeIf { it.isNotBlank() }
                                                    ?: ""
                                            }
                                            val candPhone = app.phone.takeIf { !it.contains("@") && it.isNotBlank() }
                                                ?: candidateContactMap[app.applicantUid]?.second?.takeIf { it.isNotBlank() }
                                                ?: candidateContactMap[app.uid]?.second?.takeIf { it.isNotBlank() }
                                                ?: ""

                                            // Email ID with copy button
                                            if (candEmail.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Email,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = candEmail,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Candidate Email", candEmail))
                                                            Toast.makeText(context, "Email copied: $candEmail", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", modifier = Modifier.size(11.dp))
                                                    }
                                                }
                                            }

                                            // Phone number with copy and call dialer
                                            if (candPhone.isNotBlank()) {
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
                                                        text = candPhone,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Candidate Phone", candPhone))
                                                            Toast.makeText(context, "Phone copied: $candPhone", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Phone", modifier = Modifier.size(11.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            try {
                                                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$candPhone"))
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
    var defaultWinnerUidForPayout by remember { mutableStateOf("") }
    var selectedAppForRefund by remember { mutableStateOf<CustomProfileApplication?>(null) }
    var defaultRefundUid by remember { mutableStateOf("") }
    var defaultRefundAmount by remember { mutableStateOf("") }
    var defaultRefundReason by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Host Result, 1 = Candidate Result

    val hostResultApps = applications.filter {
        it.isHostResultSubmitted || (it.isResultSubmitted && it.resultSubmittedBy.equals("Host", ignoreCase = true)) || it.hostResultScreenshot.isNotBlank()
    }
    val candidateResultApps = applications.filter {
        it.isCandidateResultSubmitted || (it.isResultSubmitted && !it.resultSubmittedBy.equals("Host", ignoreCase = true)) || it.candidateResultScreenshot.isNotBlank() || it.isReported || it.reportMediaUrl.isNotBlank()
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
                            text = "MATCH RESULTS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Verify Host & Candidate match results and settle prize payouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Room Summary Card (No Host Info)
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
                        Text(
                            text = profile.name.ifBlank { "Custom Room Match" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${profile.game} • ${profile.type} • Mode: ${profile.mode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Tab Selector: Host Result vs Candidate Result
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
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Host Result (${hostResultApps.size})", fontWeight = FontWeight.Bold)
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
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Candidate Result (${candidateResultApps.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content Area
            if (selectedTab == 0) {
                // 1. Host Result Tab
                if (hostResultApps.isEmpty()) {
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
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Host Results Submitted Yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "When the room host uploads their match result screenshot and declares the winner, it will appear here for admin review and prize payout.",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(hostResultApps, key = { _, item -> "host_res_${item.id}" }) { index, app ->
                            val payoutAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
                            val winnerName = app.hostWinnerName.ifBlank { app.winnerName }.ifBlank { app.candidateName }
                            val winnerUid = app.hostWinnerUid.ifBlank { app.winnerUid }.ifBlank { app.uid }
                            val resultPhoto = app.hostResultScreenshot.ifBlank { app.resultScreenshot }
                            val mediaUrl = app.reportMediaUrl
                            val isVideo = mediaUrl.contains(".mp4", ignoreCase = true) ||
                                          mediaUrl.contains(".mov", ignoreCase = true) ||
                                          mediaUrl.contains(".mkv", ignoreCase = true) ||
                                          mediaUrl.contains(".webm", ignoreCase = true) ||
                                          mediaUrl.contains("video", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Item Top Header
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
                                                imageVector = Icons.Default.SportsEsports,
                                                contentDescription = null,
                                                tint = TrophyGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Host Result #${index + 1}",
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

                                    // Candidate Information Box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Candidate: ${app.candidateName}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "(UID: ${app.uid})",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Candidate UID", app.uid))
                                                            Toast.makeText(context, "Candidate UID copied: ${app.uid}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "SUBMITTED BY HOST",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (app.phone.isNotBlank() || app.email.isNotBlank()) {
                                                val candContact = app.phone.ifBlank { app.email }
                                                Text(
                                                    text = "Candidate Contact: $candContact",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 1. SEPARATE SECTION: MATCH RESULT INFO & WINNER PROOF
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SuccessGreen.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "MATCH RESULT INFORMATION",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = SuccessGreen
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Declared Winner by Host:",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "$winnerName (UID: $winnerUid)",
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

                                            // Host Result Screenshot Photo
                                            if (resultPhoto.isNotBlank()) {
                                                Text(
                                                    text = "📸 Match Result Screenshot Photo (Click to zoom):",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(190.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { selectedImageForPreview = resultPhoto }
                                                ) {
                                                    AsyncImage(
                                                        model = resultPhoto,
                                                        contentDescription = "Host Match Result Screenshot",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color.Black.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
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
                                    }

                                    // 2. SEPARATE SECTION: DISPUTE / REPORT INFO (if present)
                                    if (app.isReported || app.reportReason.isNotBlank() || app.reportDescription.isNotBlank() || mediaUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AlertRed.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "DISPUTE & REPORT DETAILS",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = AlertRed
                                                    )
                                                }

                                                if (app.reportReason.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = AlertRed.copy(alpha = 0.15f),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "Reason: ${app.reportReason}",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = AlertRed,
                                                            modifier = Modifier.padding(6.dp)
                                                        )
                                                    }
                                                }

                                                if (app.reportDescription.isNotBlank()) {
                                                    Text(
                                                        text = "Description: ${app.reportDescription}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (mediaUrl.isNotBlank() && mediaUrl != resultPhoto) {
                                                    if (isVideo) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color.Black.copy(alpha = 0.85f),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { openMediaInSystem(context, mediaUrl, true) }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(10.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(Icons.Default.PlayCircle, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(32.dp))
                                                                Column {
                                                                    Text("🎬 Watch Evidence Video Proof", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                                    Text("(Tap to open and play video)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(160.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                .clickable { selectedImageForPreview = mediaUrl }
                                                        ) {
                                                            AsyncImage(
                                                                model = mediaUrl,
                                                                contentDescription = "Report Photo Proof",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 3. SEPARATE SECTION: ADMIN SETTLEMENT & REFUND ACTIONS
                                    Text(
                                        text = "Admin Payout & Settle Decision:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

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
                                            // Pay Prize Fully Option
                                            Button(
                                                onClick = {
                                                    defaultWinnerUidForPayout = winnerUid
                                                    selectedAppForPayout = app
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pay Prize Fully (₹$payoutAmount) to Winner", fontWeight = FontWeight.Bold)
                                            }

                                            // Refund Option
                                            Button(
                                                onClick = {
                                                    defaultRefundUid = app.uid
                                                    defaultRefundAmount = payoutAmount.toString()
                                                    defaultRefundReason = "Host Result Dispute / Refund"
                                                    selectedAppForRefund = app
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Refund Entry Fee (₹$payoutAmount)", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. Candidate Result Tab
                if (candidateResultApps.isEmpty()) {
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
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Candidate Results Submitted Yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "When candidates upload their match result screenshot proof or report details, they will be listed here for admin review and prize payout.",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(candidateResultApps, key = { _, item -> "cand_res_${item.id}" }) { index, app ->
                            val payoutAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
                            val winnerName = app.candidateWinnerName.ifBlank { app.winnerName }.ifBlank { app.candidateName }
                            val winnerUid = app.candidateWinnerUid.ifBlank { app.winnerUid }.ifBlank { app.uid }
                            val resultPhoto = app.candidateResultScreenshot.ifBlank { app.resultScreenshot }
                            val mediaUrl = app.reportMediaUrl
                            val isVideo = mediaUrl.contains(".mp4", ignoreCase = true) ||
                                          mediaUrl.contains(".mov", ignoreCase = true) ||
                                          mediaUrl.contains(".mkv", ignoreCase = true) ||
                                          mediaUrl.contains(".webm", ignoreCase = true) ||
                                          mediaUrl.contains("video", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, if (app.isReported) AlertRed.copy(alpha = 0.7f) else SuccessGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Item Top Header
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
                                                imageVector = if (app.isReported) Icons.Default.Warning else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (app.isReported) AlertRed else TrophyGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (app.isReported) "Candidate Result & Report #${index + 1}" else "Candidate Result #${index + 1}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (app.winningAmountSent) SuccessGreen.copy(alpha = 0.2f) else if (app.isReported) AlertRed.copy(alpha = 0.2f) else TrophyGold.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (app.winningAmountSent) "PRIZE PAID" else if (app.isReported) "REPORT FILED" else "AWAITING PAYOUT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (app.winningAmountSent) SuccessGreen else if (app.isReported) AlertRed else TrophyGold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Candidate Information Box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Candidate: ${app.candidateName}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "(UID: ${app.uid})",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Candidate UID", app.uid))
                                                            Toast.makeText(context, "Candidate UID copied: ${app.uid}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "BY CANDIDATE",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (app.phone.isNotBlank() || app.email.isNotBlank()) {
                                                val candContact = app.phone.ifBlank { app.email }
                                                Text(
                                                    text = "Candidate Contact: $candContact",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 1. SEPARATE SECTION: MATCH RESULT INFO & RESULT PHOTO
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SuccessGreen.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "MATCH RESULT INFORMATION",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = SuccessGreen
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Declared Winner by Candidate:",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "$winnerName (UID: $winnerUid)",
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

                                            // Candidate Match Result Screenshot Photo
                                            if (resultPhoto.isNotBlank()) {
                                                Text(
                                                    text = "📸 Match Result Screenshot Photo (Click to zoom):",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(190.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { selectedImageForPreview = resultPhoto }
                                                ) {
                                                    AsyncImage(
                                                        model = resultPhoto,
                                                        contentDescription = "Candidate Match Result Screenshot",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color.Black.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(6.dp)
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
                                    }

                                    // 2. SEPARATE SECTION: DISPUTE & REPORT EVIDENCE (Distinct Red Themed Block)
                                    if (app.isReported || app.reportReason.isNotBlank() || app.reportDescription.isNotBlank() || mediaUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AlertRed.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = "DISPUTE & FRAUD REPORT EVIDENCE",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = AlertRed
                                                    )
                                                }

                                                if (app.reportReason.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = AlertRed.copy(alpha = 0.15f),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "Dispute Reason: ${app.reportReason}",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            color = AlertRed,
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                    }
                                                }

                                                if (app.reportDescription.isNotBlank()) {
                                                    Text(
                                                        text = "Description: ${app.reportDescription}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Video or Photo Evidence for Dispute
                                                if (mediaUrl.isNotBlank() && mediaUrl != resultPhoto) {
                                                    if (isVideo) {
                                                        Text(
                                                            text = "🎥 Report Video Evidence Proof:",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color.Black.copy(alpha = 0.9f),
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { openMediaInSystem(context, mediaUrl, true) }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlayCircle,
                                                                    contentDescription = "Play Video",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(36.dp)
                                                                )
                                                                Column {
                                                                    Text(
                                                                        text = "🎬 Watch Dispute Evidence Video",
                                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                        color = Color.White
                                                                    )
                                                                    Text(
                                                                        text = "Tap to open & play video evidence in player",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "📸 Report Photo Evidence Proof (Click to zoom):",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                .clickable { selectedImageForPreview = mediaUrl }
                                                        ) {
                                                            AsyncImage(
                                                                model = mediaUrl,
                                                                contentDescription = "Report Proof Image",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color.Black.copy(alpha = 0.7f),
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomEnd)
                                                                    .padding(6.dp)
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
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 3. SEPARATE SECTION: ADMIN SETTLEMENT & REFUND ACTIONS (In the last / bottom)
                                    Text(
                                        text = "Admin Payout & Settle Decision:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

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
                                                    text = "Prize of ₹$payoutAmount Fully Credited to Winner UID: ${app.winningAmountSentTo.ifBlank { winnerUid }}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Primary Button 1: Pay Prize Fully
                                            Button(
                                                onClick = {
                                                    defaultWinnerUidForPayout = winnerUid
                                                    selectedAppForPayout = app
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                            ) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pay Prize Fully (₹$payoutAmount) to Winner", fontWeight = FontWeight.Bold)
                                            }

                                            // Primary Button 2: Refund Entry Fee Option
                                            Button(
                                                onClick = {
                                                    defaultRefundUid = app.uid
                                                    defaultRefundAmount = payoutAmount.toString()
                                                    defaultRefundReason = if (app.isReported) "Candidate Dispute Approved - Full Refund" else "Match Entry Fee Refund"
                                                    selectedAppForRefund = app
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Refund Entry Fee (₹$payoutAmount)", fontWeight = FontWeight.Bold)
                                            }

                                            if (app.isReported) {
                                                OutlinedButton(
                                                    onClick = {
                                                        onDismissReport(app)
                                                        Toast.makeText(context, "Report marked resolved", Toast.LENGTH_SHORT).show()
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
        var winnerUidInput by remember { mutableStateOf(defaultWinnerUidForPayout.ifBlank { app.winnerUid.ifBlank { app.uid } }) }

        AlertDialog(
            onDismissRequest = { selectedAppForPayout = null },
            title = { Text("Pay Prize Fully to Winner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Candidate: ${app.candidateName} (UID: ${app.uid})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
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
                    Text("Confirm & Pay Prize Fully")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForPayout = null }) { Text("Cancel") }
            }
        )
    }

    // Refund Confirmation Dialog
    if (selectedAppForRefund != null) {
        val app = selectedAppForRefund!!
        val defaultAmount = app.payout.toIntOrNull() ?: profile.payout.toIntOrNull() ?: 50
        var amountInput by remember { mutableStateOf(defaultRefundAmount.ifBlank { defaultAmount.toString() }) }
        var targetUidInput by remember { mutableStateOf(defaultRefundUid.ifBlank { app.uid }) }
        var reasonInput by remember { mutableStateOf(defaultRefundReason.ifBlank { "Match Entry Fee Refund" }) }

        AlertDialog(
            onDismissRequest = { selectedAppForRefund = null },
            title = { Text("Process Entry Fee Refund", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Candidate: ${app.candidateName} (UID: ${app.uid})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = targetUidInput,
                        onValueChange = { targetUidInput = it },
                        label = { Text("Refund to Player UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Refund Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Refund Reason / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toIntOrNull() ?: defaultAmount
                        if (onRefundOrCredit != null) {
                            onRefundOrCredit(targetUidInput.trim(), amount, reasonInput.trim())
                        }
                        Toast.makeText(context, "₹$amount refunded to UID: $targetUidInput!", Toast.LENGTH_SHORT).show()
                        selectedAppForRefund = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Confirm & Process Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForRefund = null }) { Text("Cancel") }
            }
        )
    }
}
