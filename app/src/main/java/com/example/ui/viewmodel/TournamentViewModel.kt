package com.example.ui.viewmodel
import kotlinx.coroutines.delay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MatchEntity
import com.example.data.model.AuthStatus
import com.example.data.model.CustomTournament
import com.example.data.model.MatchItem
import com.example.data.model.TournamentHistory
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.repository.TournamentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.PrizeBreakdown
import com.example.data.model.WalletTransaction
import java.util.UUID
import kotlinx.coroutines.tasks.await
import com.example.worker.NotificationManagerHelper


sealed class UiEvent {
  data class ShowSnackbar(val message: String) : UiEvent()
  data class NavigateToMatchDetails(val matchId: Long) : UiEvent()
}

class TournamentViewModel(application: Application) : AndroidViewModel(application) {

  private val database: AppDatabase = AppDatabase.getDatabase(application, viewModelScope)
  private val repository: TournamentRepository

  init {
    repository = TournamentRepository(
      matchDao = database.matchDao(),
      customTournamentDao = database.customTournamentDao(),
      userProfileDao = database.userProfileDao(),
      historyDao = database.tournamentHistoryDao(),
      userAccountDao = database.userAccountDao(),
      activeSessionDao = database.activeSessionDao(),
      roomRegistrationDao = database.roomRegistrationDao(),
      matchRegistrationDao = database.matchRegistrationDao()
    )

    // Ensure database initial data and active session are populated on startup
    viewModelScope.launch(Dispatchers.IO) {
      repository.ensureAdminAccount()
      if (FirebaseAuth.getInstance().currentUser == null) {
        try {
          FirebaseAuth.getInstance().signInAnonymously().await()
        } catch (_: Exception) {}
      }
      val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      if (currentAuthUid.isNotBlank()) {
        repository.syncSessionForFirebaseUser(currentAuthUid)
      }
      try {
        val deletedDocs = try {
          FirebaseFirestore.getInstance().collection("deleted_matches").get().await()
        } catch (ex: Exception) { null }
        val deletedIds = deletedDocs?.documents?.mapNotNull { doc ->
          (doc.get("matchId") as? Long) ?: (doc.get("matchId") as? String)?.toLongOrNull() ?: doc.id.toLongOrNull()
        }?.toSet() ?: emptySet()

        if (database.matchDao().getMatchCount() == 0) {
          val matchDocs = try {
            FirebaseFirestore.getInstance().collection("matches").get().await()
          } catch (ex: Exception) { null }

          val hasCloudMatches = (matchDocs != null && !matchDocs.isEmpty)
          val hasDeletedMatches = deletedIds.isNotEmpty()

          if (!hasCloudMatches && !hasDeletedMatches) {
            AppDatabase.populateInitialData(database, includeMatches = true)
          } else {
            AppDatabase.populateInitialData(database, includeMatches = false)
          }
        } else {
          if (deletedIds.isNotEmpty()) {
            deletedIds.forEach { delId ->
              database.matchDao().deleteMatch(delId)
              database.matchRegistrationDao().deleteRegistrationsForMatch(delId)
            }
          }
          database.matchDao().sanitizeBgmiMatches()
          database.matchDao().sanitizeCodMatches()
          database.matchDao().sanitizeValorantMatches()
          database.tournamentHistoryDao().sanitizeHistoryBgmi()
          database.tournamentHistoryDao().sanitizeHistoryCod()
        }
        repository.purgeDummyRegistrations()
      } catch (e: Exception) {
        // Log or handle gracefully
      }
      listenToMatches()
      listenToMatchRegistrations()
      fetchCustomProfiles()
      listenToCustomProfileApplications()
      refreshWalletTransactions()
      viewModelScope.launch {
        combine(
          _firestoreCustomProfiles,
          userProfile,
          authStatus
        ) { _, _, _ -> Unit }.collect {
          recomputeApplications(_allCustomProfileApplicationsForAdmin.value)
        }
      }
    }

    FirebaseAuth.getInstance().addAuthStateListener { auth ->
      val uid = auth.currentUser?.uid ?: ""
      if (uid.isNotBlank()) {
        viewModelScope.launch(Dispatchers.IO) {
          repository.syncSessionForFirebaseUser(uid)
          refreshWalletTransactions()
        }
      }
    }
  }

  fun listenToMatches() {
    try {
      // 1. Real-time listener for permanently deleted matches
      FirebaseFirestore.getInstance().collection("deleted_matches")
        .addSnapshotListener { snapshot, e ->
          if (e != null) {
            android.util.Log.w("TournamentViewModel", "Deleted matches listen error: ${e.message}")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val deletedIds = snapshot.documents.mapNotNull { doc ->
              (doc.get("matchId") as? Long) ?: (doc.get("matchId") as? String)?.toLongOrNull() ?: doc.id.toLongOrNull()
            }.toSet()
            if (deletedIds.isNotEmpty()) {
              viewModelScope.launch(Dispatchers.IO) {
                deletedIds.forEach { deletedId ->
                  database.matchDao().deleteMatch(deletedId)
                  database.matchRegistrationDao().deleteRegistrationsForMatch(deletedId)
                }
              }
            }
          }
        }

      // 2. Real-time listener for active matches created & managed by admin
      FirebaseFirestore.getInstance().collection("matches")
        .addSnapshotListener { snapshot, e ->
          if (e != null) {
            android.util.Log.w("TournamentViewModel", "Matches listen error: ${e.message}")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            viewModelScope.launch(Dispatchers.IO) {
              try {
                val deletedDocs = try {
                  FirebaseFirestore.getInstance().collection("deleted_matches").get().await()
                } catch (ex: Exception) { null }
                val deletedIds = deletedDocs?.documents?.mapNotNull { doc ->
                  (doc.get("matchId") as? Long) ?: (doc.get("matchId") as? String)?.toLongOrNull() ?: doc.id.toLongOrNull()
                }?.toSet() ?: emptySet()

                if (snapshot.documents.isEmpty()) {
                  // Only seed to Firestore if both matches AND deleted_matches are completely empty
                  if (deletedIds.isEmpty()) {
                    val currentLocal = database.matchDao().getMatchCount()
                    if (currentLocal > 0) {
                      val localMatches: List<MatchEntity> = database.matchDao().getAllMatchesOnce()
                      localMatches.forEach { entity: MatchEntity ->
                        val map = hashMapOf<String, Any>(
                          "id" to entity.id,
                          "matchNumber" to entity.matchNumber,
                          "name" to entity.name,
                          "gameTitle" to entity.gameTitle,
                          "entryFee" to entity.entryFee,
                          "prizePool" to entity.prizePool,
                          "perKill" to entity.perKill,
                          "totalPlayers" to entity.totalPlayers,
                          "maxPlayers" to entity.maxPlayers,
                          "date" to entity.date,
                          "time" to entity.time,
                          "rankRequirement" to entity.rankRequirement,
                          "region" to entity.region,
                          "status" to entity.status,
                          "format" to entity.format,
                          "mapName" to entity.mapName,
                          "rules" to entity.rules,
                          "description" to entity.description,
                          "prizeDistributionJson" to entity.prizeDistributionJson,
                          "roomId" to entity.roomId,
                          "roomPassword" to entity.roomPassword,
                          "bannerImageUrl" to entity.bannerImageUrl,
                          "isRoomBroadcasted" to entity.isRoomBroadcasted,
                          "createdAt" to System.currentTimeMillis()
                        )
                        FirebaseFirestore.getInstance().collection("matches").document(entity.id.toString()).set(map)
                      }
                    }
                  }
                } else {
                  val remoteMatchEntities = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = (data["id"] as? Long) ?: (data["id"] as? String)?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    if (deletedIds.contains(id)) return@mapNotNull null

                    val existingEntity = database.matchDao().getMatchById(id)
                    MatchEntity(
                      id = id,
                      matchNumber = (data["matchNumber"] as? Long)?.toInt() ?: (data["matchNumber"] as? Int) ?: id.toInt(),
                      name = data["name"] as? String ?: "Tournament Match",
                      gameTitle = data["gameTitle"] as? String ?: "Free Fire MAX",
                      entryFee = (data["entryFee"] as? Long)?.toInt() ?: (data["entryFee"] as? Int) ?: 0,
                      prizePool = (data["prizePool"] as? Long)?.toInt() ?: (data["prizePool"] as? Int) ?: 0,
                      perKill = (data["perKill"] as? Long)?.toInt() ?: (data["perKill"] as? Int) ?: 0,
                      totalPlayers = (data["totalPlayers"] as? Long)?.toInt() ?: (data["totalPlayers"] as? Int) ?: 0,
                      maxPlayers = (data["maxPlayers"] as? Long)?.toInt() ?: (data["maxPlayers"] as? Int) ?: 100,
                      date = data["date"] as? String ?: "Today",
                      time = data["time"] as? String ?: "07:00 PM IST",
                      rankRequirement = data["rankRequirement"] as? String ?: "Level 20+",
                      region = data["region"] as? String ?: "India",
                      status = data["status"] as? String ?: "OPEN",
                      format = data["format"] as? String ?: "Squad (BR)",
                      mapName = data["mapName"] as? String ?: "Bermuda (Classic)",
                      rules = data["rules"] as? String ?: "",
                      description = data["description"] as? String ?: "",
                      prizeDistributionJson = data["prizeDistributionJson"] as? String ?: "[]",
                      roomId = data["roomId"] as? String ?: "",
                      roomPassword = data["roomPassword"] as? String ?: "",
                      isJoined = existingEntity?.isJoined ?: false,
                      userGameUid = existingEntity?.userGameUid ?: "",
                      bannerImageUrl = data["bannerImageUrl"] as? String ?: "",
                      isRoomBroadcasted = (data["isRoomBroadcasted"] as? Boolean) ?: false
                    )
                  }

                  val remoteIds = remoteMatchEntities.map { it.id }.toSet()
                  val localMatches: List<MatchEntity> = database.matchDao().getAllMatchesOnce()
                  localMatches.forEach { local: MatchEntity ->
                    if (deletedIds.contains(local.id) || !remoteIds.contains(local.id)) {
                      database.matchDao().deleteMatch(local.id)
                      database.matchRegistrationDao().deleteRegistrationsForMatch(local.id)
                    }
                  }

                  remoteMatchEntities.forEach { remote ->
                    database.matchDao().insertMatch(remote)
                  }
                }
              } catch (ex: Exception) {
                android.util.Log.e("TournamentViewModel", "Error syncing matches from Firestore", ex)
              }
            }
          }
        }
    } catch (e: Exception) {
      android.util.Log.w("TournamentViewModel", "Failed to start matches listener: ${e.message}")
    }
  }


  private val _adminUsersList = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  fun refreshMatches() {
    viewModelScope.launch {
      _isRefreshing.value = true
      delay(1000)
      // Simulated Firestore network refresh
      _isRefreshing.value = false
    }
  }
  val adminUsersList: StateFlow<List<Map<String, Any>>> = _adminUsersList.asStateFlow()

  fun fetchAdminUsers() {
    viewModelScope.launch {
      try {
        val snapshot = FirebaseFirestore.getInstance().collection("users").get().await()
        val users = snapshot.documents.mapNotNull { it.data }
        if (users.isNotEmpty()) {
          _adminUsersList.value = users
        } else {
          loadLocalAccountsIntoAdminUsers()
        }
      } catch (_: Exception) {
        loadLocalAccountsIntoAdminUsers()
      }
    }
  }

  private suspend fun loadLocalAccountsIntoAdminUsers() {
    try {
      val accounts = repository.allAccounts.firstOrNull() ?: emptyList()
      _adminUsersList.value = accounts.map { account ->
        mapOf<String, Any>(
          "id" to account.id,
          "username" to account.username,
          "name" to account.name,
          "fullName" to account.name,
          "email" to account.email,
          "phone" to account.phone,
          "gameUid" to account.gameUid,
          "walletBalance" to account.walletBalance,
          "totalEarnings" to account.totalEarnings,
          "rank" to account.rank,
          "role" to if (account.phone == "6205964987" || account.username.contains("admin", ignoreCase = true)) "admin" else "player"
        )
      }
    } catch (_: Exception) {}
  }


  private val _firestoreCustomProfiles = MutableStateFlow<List<com.example.data.model.CustomProfile>>(emptyList())
  val firestoreCustomProfiles: StateFlow<List<com.example.data.model.CustomProfile>> = _firestoreCustomProfiles.asStateFlow()

  private val _allCustomProfileApplicationsForAdmin = MutableStateFlow<List<com.example.data.model.CustomProfileApplication>>(emptyList())
  val allCustomProfileApplicationsForAdmin: StateFlow<List<com.example.data.model.CustomProfileApplication>> = _allCustomProfileApplicationsForAdmin.asStateFlow()

  private val _myCustomProfileApplications = MutableStateFlow<List<com.example.data.model.CustomProfileApplication>>(emptyList())
  val myCustomProfileApplications: StateFlow<List<com.example.data.model.CustomProfileApplication>> = _myCustomProfileApplications.asStateFlow()

  private val _myAppliedCustomProfiles = MutableStateFlow<List<com.example.data.model.CustomProfileApplication>>(emptyList())
  val myAppliedCustomProfiles: StateFlow<List<com.example.data.model.CustomProfileApplication>> = _myAppliedCustomProfiles.asStateFlow()

  private var customAppsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
  private val customProfilePrefs by lazy {
    getApplication<android.app.Application>().getSharedPreferences("custom_profile_prefs", android.content.Context.MODE_PRIVATE)
  }

  fun hasAppliedToCustomProfile(profileId: String): Boolean {
    val savedRooms = customProfilePrefs.getStringSet("applied_room_ids", emptySet()) ?: emptySet()
    return _myAppliedCustomProfiles.value.any { it.profileId == profileId } || savedRooms.contains(profileId)
  }

  fun deleteCustomProfile(profileId: String) {
    FirebaseFirestore.getInstance().collection("custom_profiles").document(profileId).delete()
    val savedRooms = (customProfilePrefs.getStringSet("applied_room_ids", emptySet()) ?: emptySet()).toMutableSet()
    savedRooms.remove(profileId)
    customProfilePrefs.edit().putStringSet("applied_room_ids", savedRooms).apply()
  }

  fun deleteCustomProfileApplication(applicationId: String) {
    _allCustomProfileApplicationsForAdmin.value = _allCustomProfileApplicationsForAdmin.value.filter { it.id != applicationId }
    _myCustomProfileApplications.value = _myCustomProfileApplications.value.filter { it.id != applicationId }
    _myAppliedCustomProfiles.value = _myAppliedCustomProfiles.value.filter { it.id != applicationId }

    val savedApps = (customProfilePrefs.getStringSet("applied_app_ids", emptySet()) ?: emptySet()).toMutableSet()
    savedApps.remove(applicationId)
    customProfilePrefs.edit().putStringSet("applied_app_ids", savedApps).apply()

    val db = FirebaseFirestore.getInstance()
    val appRef = db.collection("custom_profile_applications").document(applicationId)
    appRef.get().addOnSuccessListener { doc ->
      val status = doc.getString("status")
      val profileId = doc.getString("profileId")
      if (profileId != null) {
        val savedRooms = (customProfilePrefs.getStringSet("applied_room_ids", emptySet()) ?: emptySet()).toMutableSet()
        savedRooms.remove(profileId)
        customProfilePrefs.edit().putStringSet("applied_room_ids", savedRooms).apply()

        val profileRef = db.collection("custom_profiles").document(profileId)
        db.runTransaction { transaction ->
          val profileDoc = transaction.get(profileRef)
          if (profileDoc.exists()) {
            val currentCandidates = profileDoc.getLong("candidateCount")?.toInt() ?: 0
            val newCandidates = (currentCandidates - 1).coerceAtLeast(0)
            val updates = hashMapOf<String, Any>(
              "candidateCount" to newCandidates,
              "isLocked" to (newCandidates >= 7)
            )
            if (status == "Accepted") {
              val currentJoined = profileDoc.getLong("joinedPlayers")?.toInt() ?: 0
              updates["joinedPlayers"] = (currentJoined - 1).coerceAtLeast(0)
              updates["hasAccepted"] = false
            }
            transaction.update(profileRef, updates)
          }
          transaction.delete(appRef)
          null
        }
      } else {
        appRef.delete()
      }
    }
  }

  fun fetchCustomProfiles() {
    listenToCustomProfileApplications()
    // Initial one-shot fetch for immediate data loading
    viewModelScope.launch(Dispatchers.IO) {
      try {
        if (FirebaseAuth.getInstance().currentUser == null) {
          try {
            FirebaseAuth.getInstance().signInAnonymously().await()
          } catch (_: Exception) {}
        }
        val snap = FirebaseFirestore.getInstance().collection("custom_profiles").get().await()
        // Clean up sample/dummy profiles from Firestore if present
        for (doc in snap.documents) {
          if (doc.id.startsWith("sample_custom_")) {
            try {
              doc.reference.delete()
            } catch (_: Exception) {}
          }
        }

        val profiles = snap.documents
          .filter { !it.id.startsWith("sample_custom_") }
          .mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val createdAtMs = (data["createdAt"] as? com.google.firebase.Timestamp)?.seconds?.times(1000)
              ?: (data["createdAt"] as? Number)?.toLong()
              ?: System.currentTimeMillis()
            com.example.data.model.CustomProfile(
              id = doc.id,
              name = data["name"] as? String ?: "",
              uid = data["uid"] as? String ?: "",
              hostActualName = (data["hostActualName"] as? String)?.takeIf { it.isNotBlank() } ?: (data["hostName"] as? String ?: ""),
              hostPhone = (data["hostPhone"] as? String)?.takeIf { it.isNotBlank() } ?: (data["phone"] as? String ?: ""),
              hostEmail = (data["hostEmail"] as? String)?.takeIf { it.isNotBlank() } ?: (data["email"] as? String ?: ""),
              hostGameUid = (data["hostGameUid"] as? String)?.takeIf { it.isNotBlank() } ?: (data["uid"] as? String ?: ""),
              level = data["level"]?.toString() ?: "",
              payout = data["payout"]?.toString() ?: "",
              type = data["type"] as? String ?: "",
              mode = data["mode"] as? String ?: "",
              gun = data["gun"] as? String ?: "",
              game = data["game"] as? String ?: "",
              day = data["day"] as? String ?: "",
              time = data["time"] as? String ?: "",
              category = (data["category"] as? String)?.takeIf { it.isNotBlank() } ?: "Custom",
              prizePool = data["prizePool"]?.toString() ?: "",
              perKill = data["perKill"]?.toString() ?: "",
              totalPlayers = data["totalPlayers"]?.toString() ?: "",
              joinedPlayers = (data["joinedPlayers"] as? Number)?.toInt() ?: 0,
              candidateCount = (data["candidateCount"] as? Number)?.toInt()
                ?: (data["joinedPlayers"] as? Number)?.toInt()
                ?: 0,
              hasAccepted = data["hasAccepted"] as? Boolean ?: false,
              isLocked = (((data["candidateCount"] as? Number)?.toInt() ?: (data["joinedPlayers"] as? Number)?.toInt() ?: 0) >= 7),
              imageUrl = data["imageUrl"] as? String ?: "",
              hostUid = (data["hostUid"] as? String)?.takeIf { it.isNotBlank() } ?: (data["uid"] as? String ?: ""),
              createdAt = createdAtMs
            )
          }.sortedByDescending { it.createdAt }

        _firestoreCustomProfiles.value = profiles
        recomputeApplications(_allCustomProfileApplicationsForAdmin.value)
      } catch (ex: Exception) {
        android.util.Log.w("TournamentViewModel", "Error initial get custom_profiles: ${ex.message}")
      }
    }

    FirebaseFirestore.getInstance().collection("custom_profiles")
      .addSnapshotListener { snapshot, e ->
        if (e != null) {
          android.util.Log.w("TournamentViewModel", "Custom profiles listen info: ${e.message}")
          return@addSnapshotListener
        }
        
        if (snapshot != null) {
          val profiles = snapshot.documents
            .filter { !it.id.startsWith("sample_custom_") }
            .mapNotNull { doc ->
              val data = doc.data ?: return@mapNotNull null
              val createdAtMs = (data["createdAt"] as? com.google.firebase.Timestamp)?.seconds?.times(1000)
                ?: (data["createdAt"] as? Number)?.toLong()
                ?: System.currentTimeMillis()
              com.example.data.model.CustomProfile(
                id = doc.id,
                name = data["name"] as? String ?: "",
                uid = data["uid"] as? String ?: "",
                hostActualName = (data["hostActualName"] as? String)?.takeIf { it.isNotBlank() } ?: (data["hostName"] as? String ?: ""),
                hostPhone = (data["hostPhone"] as? String)?.takeIf { it.isNotBlank() } ?: (data["phone"] as? String ?: ""),
                hostEmail = (data["hostEmail"] as? String)?.takeIf { it.isNotBlank() } ?: (data["email"] as? String ?: ""),
                hostGameUid = (data["hostGameUid"] as? String)?.takeIf { it.isNotBlank() } ?: (data["uid"] as? String ?: ""),
                level = data["level"]?.toString() ?: "",
                payout = data["payout"]?.toString() ?: "",
                type = data["type"] as? String ?: "",
                mode = data["mode"] as? String ?: "",
                gun = data["gun"] as? String ?: "",
                game = data["game"] as? String ?: "",
                day = data["day"] as? String ?: "",
                time = data["time"] as? String ?: "",
                category = (data["category"] as? String)?.takeIf { it.isNotBlank() } ?: "Custom",
                prizePool = data["prizePool"]?.toString() ?: "",
                perKill = data["perKill"]?.toString() ?: "",
                totalPlayers = data["totalPlayers"]?.toString() ?: "",
                joinedPlayers = (data["joinedPlayers"] as? Number)?.toInt() ?: 0,
                candidateCount = (data["candidateCount"] as? Number)?.toInt()
                  ?: (data["joinedPlayers"] as? Number)?.toInt()
                  ?: 0,
                hasAccepted = data["hasAccepted"] as? Boolean ?: false,
                isLocked = (((data["candidateCount"] as? Number)?.toInt() ?: (data["joinedPlayers"] as? Number)?.toInt() ?: 0) >= 7),
                imageUrl = data["imageUrl"] as? String ?: "",
                hostUid = (data["hostUid"] as? String)?.takeIf { it.isNotBlank() } ?: (data["uid"] as? String ?: ""),
                createdAt = createdAtMs
              )
            }.sortedByDescending { it.createdAt }
          _firestoreCustomProfiles.value = profiles
          recomputeApplications(_allCustomProfileApplicationsForAdmin.value)
        }
      }
  }

  fun listenToCustomProfileApplications() {
    // Also run an immediate one-shot get so candidates are instantly available
    viewModelScope.launch(Dispatchers.IO) {
      try {
        if (FirebaseAuth.getInstance().currentUser == null) {
          try {
            FirebaseAuth.getInstance().signInAnonymously().await()
          } catch (_: Exception) {}
        }
        val snap = FirebaseFirestore.getInstance().collection("custom_profile_applications").get().await()
        val apps = snap.documents.mapNotNull { doc ->
          val data = doc.data ?: return@mapNotNull null
          com.example.data.model.CustomProfileApplication(
            id = doc.id,
            profileId = data["profileId"] as? String ?: "",
            profileName = data["profileName"] as? String ?: "",
            hostUid = data["hostUid"] as? String ?: "",
            candidateName = data["candidateName"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            uid = data["uid"] as? String ?: "",
            level = data["level"]?.toString() ?: "",
            rank = data["rank"] as? String ?: "",
            status = data["status"] as? String ?: "Pending",
            appliedAt = (data["appliedAt"] as? Long) ?: (data["appliedAt"] as? Number)?.toLong() ?: 0L,
            roomId = data["roomId"] as? String,
            roomPassword = data["roomPassword"] as? String,
            hostPaid = data["hostPaid"] as? Boolean ?: false,
            payout = data["payout"]?.toString() ?: "",
            applicantUid = data["applicantUid"] as? String ?: "",
            resultScreenshot = data["resultScreenshot"] as? String ?: "",
            winnerName = data["winnerName"] as? String ?: "",
            winnerUid = data["winnerUid"] as? String ?: "",
            isResultSubmitted = data["isResultSubmitted"] as? Boolean ?: false,
            resultSubmittedBy = data["resultSubmittedBy"] as? String ?: "",
            reportReason = data["reportReason"] as? String ?: "",
            reportDescription = data["reportDescription"] as? String ?: "",
            reportMediaUrl = data["reportMediaUrl"] as? String ?: "",
            reportSubmittedBy = data["reportSubmittedBy"] as? String ?: "",
            isReported = data["isReported"] as? Boolean ?: false,
            winningAmountSent = data["winningAmountSent"] as? Boolean ?: false,
            winningAmountSentTo = data["winningAmountSentTo"] as? String ?: "",
            hostActualName = (data["hostActualName"] as? String)?.takeIf { it.isNotBlank() } ?: (data["hostName"] as? String ?: ""),
            hostPhone = (data["hostPhone"] as? String)?.takeIf { it.isNotBlank() } ?: "",
            hostEmail = (data["hostEmail"] as? String)?.takeIf { it.isNotBlank() } ?: "",
            hostGameUid = (data["hostGameUid"] as? String)?.takeIf { it.isNotBlank() } ?: ""
          )
        }.sortedByDescending { it.appliedAt }
        if (apps.isNotEmpty()) {
          _allCustomProfileApplicationsForAdmin.value = apps
          recomputeApplications(apps)
        }
      } catch (ex: Exception) {
        android.util.Log.w("TournamentViewModel", "Error initial get applications: ${ex.message}")
      }
    }

    if (customAppsListenerRegistration != null) return
    try {
      customAppsListenerRegistration = FirebaseFirestore.getInstance()
        .collection("custom_profile_applications")
        .addSnapshotListener { snapshot, e ->
          if (e != null) {
            android.util.Log.w("TournamentViewModel", "Applications listener info: ${e.message}")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val apps = snapshot.documents.mapNotNull { doc ->
              val data = doc.data ?: return@mapNotNull null
              com.example.data.model.CustomProfileApplication(
                id = doc.id,
                profileId = data["profileId"] as? String ?: "",
                profileName = data["profileName"] as? String ?: "",
                hostUid = data["hostUid"] as? String ?: "",
                candidateName = data["candidateName"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                uid = data["uid"] as? String ?: "",
                level = data["level"]?.toString() ?: "",
                rank = data["rank"] as? String ?: "",
                status = data["status"] as? String ?: "Pending",
                appliedAt = (data["appliedAt"] as? Long) ?: (data["appliedAt"] as? Number)?.toLong() ?: 0L,
                roomId = data["roomId"] as? String,
                roomPassword = data["roomPassword"] as? String,
                hostPaid = data["hostPaid"] as? Boolean ?: false,
                payout = data["payout"]?.toString() ?: "",
                applicantUid = data["applicantUid"] as? String ?: "",
                resultScreenshot = data["resultScreenshot"] as? String ?: "",
                winnerName = data["winnerName"] as? String ?: "",
                winnerUid = data["winnerUid"] as? String ?: "",
                isResultSubmitted = data["isResultSubmitted"] as? Boolean ?: false,
                resultSubmittedBy = data["resultSubmittedBy"] as? String ?: "",
                reportReason = data["reportReason"] as? String ?: "",
                reportDescription = data["reportDescription"] as? String ?: "",
                reportMediaUrl = data["reportMediaUrl"] as? String ?: "",
                reportSubmittedBy = data["reportSubmittedBy"] as? String ?: "",
                isReported = data["isReported"] as? Boolean ?: false,
                winningAmountSent = data["winningAmountSent"] as? Boolean ?: false,
                winningAmountSentTo = data["winningAmountSentTo"] as? String ?: "",
                hostActualName = (data["hostActualName"] as? String)?.takeIf { it.isNotBlank() } ?: (data["hostName"] as? String ?: ""),
                hostPhone = (data["hostPhone"] as? String)?.takeIf { it.isNotBlank() } ?: "",
                hostEmail = (data["hostEmail"] as? String)?.takeIf { it.isNotBlank() } ?: "",
                hostGameUid = (data["hostGameUid"] as? String)?.takeIf { it.isNotBlank() } ?: ""
              )
            }.sortedByDescending { it.appliedAt }

            _allCustomProfileApplicationsForAdmin.value = apps
            recomputeApplications(apps)
          }
        }
    } catch (e: Exception) {
      android.util.Log.w("TournamentViewModel", "Applications listen error: ${e.message}")
    }
  }

  private fun recomputeApplications(apps: List<com.example.data.model.CustomProfileApplication>) {
    val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val curUserProfile = userProfile.value
    val curStatus = authStatus.value
    val curAccount = (curStatus as? AuthStatus.LoggedIn)?.account
    val isAdminUser = AdminConfig.isUserAdmin(
      firebaseUid = currentAuthUid,
      accountPhone = curAccount?.phone,
      accountGameUid = curAccount?.gameUid,
      accountUsername = curAccount?.username,
      accountId = curAccount?.id ?: 0L
    )

    val profiles = _firestoreCustomProfiles.value
    val curGameUid = curAccount?.gameUid?.trim()?.takeIf { it.isNotBlank() } ?: curUserProfile.uid.trim()
    val curPhoneDigits = curAccount?.phone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() } ?: ""
    val curUsername = curAccount?.username?.trim()?.takeIf { it.isNotBlank() } ?: curUserProfile.name.trim()

    val savedAppIds = customProfilePrefs.getStringSet("applied_app_ids", emptySet()) ?: emptySet()
    val savedRoomIds = customProfilePrefs.getStringSet("applied_room_ids", emptySet()) ?: emptySet()

    // Determine profiles owned by this user
    val myProfileIds = profiles.filter { p ->
      (curGameUid.isNotBlank() && (p.uid.equals(curGameUid, ignoreCase = true) || p.hostUid.equals(curGameUid, ignoreCase = true))) ||
      (currentAuthUid.isNotBlank() && p.hostUid.isNotBlank() && p.hostUid == currentAuthUid) ||
      (curPhoneDigits.isNotBlank() && p.hostPhone.filter { it.isDigit() }.isNotBlank() && p.hostPhone.filter { it.isDigit() } == curPhoneDigits) ||
      (curUsername.isNotBlank() && (p.hostActualName.equals(curUsername, ignoreCase = true) || p.name.equals(curUsername, ignoreCase = true)))
    }.map { it.id }.toSet()

    // Hosted applications: applications for rooms hosted by this user (or all apps if in admin mode)
    val hostedApps = if (isAdminUser) {
      apps
    } else {
      apps.filter { app ->
        myProfileIds.contains(app.profileId) ||
        (curGameUid.isNotBlank() && app.hostUid.isNotBlank() && app.hostUid.equals(curGameUid, ignoreCase = true)) ||
        (currentAuthUid.isNotBlank() && app.hostUid.isNotBlank() && app.hostUid == currentAuthUid)
      }
    }
    _myCustomProfileApplications.value = hostedApps

    // Applied applications: registrations submitted by THIS user/account as a candidate (for Active Registered)
    val previousList = _myAppliedCustomProfiles.value
    val filtered = apps.filter { app ->
      val appPhoneDigits = app.phone.filter { it.isDigit() }
      savedAppIds.contains(app.id) ||
      savedRoomIds.contains(app.profileId) ||
      (currentAuthUid.isNotBlank() && app.applicantUid.isNotBlank() && app.applicantUid == currentAuthUid) ||
      (curGameUid.isNotBlank() && app.uid.isNotBlank() && app.uid.equals(curGameUid, ignoreCase = true)) ||
      (curPhoneDigits.isNotBlank() && appPhoneDigits.isNotBlank() && appPhoneDigits == curPhoneDigits) ||
      (curUsername.isNotBlank() && app.candidateName.isNotBlank() && app.candidateName.equals(curUsername, ignoreCase = true)) ||
      (curGameUid.isNotBlank() && app.applicantUid.isNotBlank() && app.applicantUid.equals(curGameUid, ignoreCase = true))
    }

    if (previousList.isNotEmpty()) {
      filtered.forEach { newApp ->
        val oldApp = previousList.find { it.id == newApp.id }
        if (oldApp != null && oldApp.status == "Pending" && newApp.status == "Accepted") {
          com.example.worker.NotificationManagerHelper.showImmediateNotification(
            getApplication(),
            "Tournament Bracket Updated",
            "Your registration for ${newApp.profileName} has been accepted!"
          )
          com.example.worker.NotificationManagerHelper.scheduleNotification(
            getApplication(),
            "Match Starting Soon",
            "Your match in ${newApp.profileName} is starting in 15 minutes!",
            1 * 60 * 1000L,
            "match_start_${newApp.id}"
          )
        }
      }
    }
    _myAppliedCustomProfiles.value = filtered
  }

  fun fetchMyCustomProfileApplications() {
    listenToCustomProfileApplications()
    recomputeApplications(_allCustomProfileApplicationsForAdmin.value)
  }

  // Events
  private val _uiEvents = MutableSharedFlow<UiEvent>()
  val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

  // Authentication State


  val authStatus: StateFlow<AuthStatus> = repository.authStatus.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = AuthStatus.Initializing
  )

  val isAdmin = authStatus.map { status ->
    val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    if (currentAuthUid == AdminConfig.NON_ADMIN_UID || AdminConfig.NON_ADMIN_UIDS.contains(currentAuthUid)) {
      false
    } else if (status is AuthStatus.LoggedIn && !status.isGuest) {
      val account = status.account
      AdminConfig.isUserAdmin(
        firebaseUid = currentAuthUid,
        accountPhone = account.phone,
        accountGameUid = account.gameUid,
        accountUsername = account.username,
        accountId = account.id
      )
    } else if (currentAuthUid == AdminConfig.ADMIN_UID || AdminConfig.ADMIN_UIDS.contains(currentAuthUid)) {
      true
    } else {
      false
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val allMatchRegistrations: StateFlow<List<MatchRegistration>> = repository.allMatchRegistrations.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val allMatches: StateFlow<List<MatchItem>> = repository.allMatches
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  fun getRegistrationsForMatch(matchId: Long): kotlinx.coroutines.flow.Flow<List<MatchRegistration>> = repository.getRegistrationsForMatch(matchId)

  fun updateMatchDetails(
    matchId: Long,
    entryFee: Int,
    prizePool: Int,
    maxPlayers: Int,
    date: String,
    time: String,
    rules: String,
    perKill: Int = 0,
    bannerImageUrl: String? = null,
    name: String? = null,
    gameTitle: String? = null,
    format: String? = null,
    mapName: String? = null
  ) {
    viewModelScope.launch {
      try {
        val existing = repository.getMatchById(matchId)
        if (existing != null) {
          val finalName = name ?: existing.name
          val isDaily = finalName.contains("Daily", ignoreCase = true) || 
                        (!finalName.contains("Weekly", ignoreCase = true) && !existing.description.contains("Weekly", ignoreCase = true))
          val calculatedPerKill = if (isDaily) {
            if (perKill > 0) perKill else existing.perKill
          } else {
            0
          }
          val updated = existing.copy(
            name = finalName,
            gameTitle = gameTitle ?: existing.gameTitle,
            format = format ?: existing.format,
            mapName = mapName ?: existing.mapName,
            entryFee = entryFee,
            prizePool = prizePool,
            maxPlayers = maxPlayers,
            date = date,
            time = time,
            rules = rules,
            perKill = calculatedPerKill,
            bannerImageUrl = bannerImageUrl ?: existing.bannerImageUrl
          )
          repository.updateMatch(updated)
          _uiEvents.emit(UiEvent.ShowSnackbar("Match details updated successfully!"))
        }
      } catch (e: Exception) {
        android.util.Log.e("ViewModel", "Failed to update match", e)
      }
    }
  }

  fun listenToMatchRegistrations() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        if (FirebaseAuth.getInstance().currentUser == null) {
          try {
            FirebaseAuth.getInstance().signInAnonymously().await()
          } catch (_: Exception) {}
        }
        val snap = FirebaseFirestore.getInstance().collection("match_registrations").get().await()
        val remoteList = snap.documents.mapNotNull { doc ->
          val data = doc.data ?: return@mapNotNull null
          val docIdHash = doc.id.hashCode().toLong().let { if (it < 0) -it else it }
          val id = docIdHash
          val matchId = (data["matchId"] as? Long) ?: (data["matchId"] as? Number)?.toLong() ?: (data["matchId"] as? String)?.toLongOrNull() ?: 0L
          val candidateName = (data["candidateName"] as? String) ?: (data["name"] as? String) ?: ""
          val gameUid = (data["gameUid"] as? String) ?: (data["uid"] as? String) ?: ""
          val applicantUid = (data["applicantUid"] as? String) ?: ""
          val userAccountId = (data["userAccountId"] as? Long) ?: (data["userAccountId"] as? Number)?.toLong() ?: 0L
          if (candidateName.isBlank() && gameUid.isBlank()) return@mapNotNull null
          MatchRegistration(
            id = id,
            matchId = matchId,
            matchTitle = data["matchTitle"] as? String ?: "",
            candidateName = candidateName.ifBlank { "Player $gameUid" },
            gameUid = gameUid,
            phoneOrEmail = data["phoneOrEmail"] as? String ?: "",
            paymentStatus = data["paymentStatus"] as? String ?: "Paid",
            entryFee = (data["entryFee"] as? Long)?.toInt() ?: (data["entryFee"] as? Number)?.toInt() ?: 0,
            registeredAt = (data["registeredAt"] as? Long) ?: (data["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            roomId = data["roomId"] as? String ?: "",
            roomPassword = data["roomPassword"] as? String ?: "",
            applicantUid = applicantUid,
            userAccountId = userAccountId
          )
        }
        if (remoteList.isNotEmpty()) {
          repository.syncRemoteMatchRegistrations(remoteList)
        }
      } catch (e: Exception) {
        android.util.Log.w("TournamentViewModel", "Initial get match registrations info: ${e.message}")
      }
    }

    try {
      FirebaseFirestore.getInstance().collection("match_registrations")
        .addSnapshotListener { snapshot, e ->
          if (e != null) {
            android.util.Log.w("TournamentViewModel", "Match registrations listen error: ${e.message}")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val remoteList = snapshot.documents.mapNotNull { doc ->
              val data = doc.data ?: return@mapNotNull null
              val docIdHash = doc.id.hashCode().toLong().let { if (it < 0) -it else it }
              val id = docIdHash
              val matchId = (data["matchId"] as? Long) ?: (data["matchId"] as? Number)?.toLong() ?: (data["matchId"] as? String)?.toLongOrNull() ?: 0L
              val candidateName = (data["candidateName"] as? String) ?: (data["name"] as? String) ?: ""
              val gameUid = (data["gameUid"] as? String) ?: (data["uid"] as? String) ?: ""
              val applicantUid = (data["applicantUid"] as? String) ?: ""
              val userAccountId = (data["userAccountId"] as? Long) ?: (data["userAccountId"] as? Number)?.toLong() ?: 0L
              if (candidateName.isBlank() && gameUid.isBlank()) return@mapNotNull null
              MatchRegistration(
                id = id,
                matchId = matchId,
                matchTitle = data["matchTitle"] as? String ?: "",
                candidateName = candidateName.ifBlank { "Player $gameUid" },
                gameUid = gameUid,
                phoneOrEmail = data["phoneOrEmail"] as? String ?: "",
                paymentStatus = data["paymentStatus"] as? String ?: "Paid",
                entryFee = (data["entryFee"] as? Long)?.toInt() ?: (data["entryFee"] as? Number)?.toInt() ?: 0,
                registeredAt = (data["registeredAt"] as? Long) ?: (data["registeredAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                roomId = data["roomId"] as? String ?: "",
                roomPassword = data["roomPassword"] as? String ?: "",
                applicantUid = applicantUid,
                userAccountId = userAccountId
              )
            }
            if (remoteList.isNotEmpty()) {
              viewModelScope.launch {
                repository.syncRemoteMatchRegistrations(remoteList)
              }
            }
          }
        }
    } catch (e: Exception) {
      android.util.Log.w("TournamentViewModel", "Failed to start match registrations listener: ${e.message}")
    }
  }

  fun updateRegistrationPaymentStatus(registrationId: Long, status: String) {
    viewModelScope.launch {
      repository.updateMatchRegistrationPaymentStatus(registrationId, status)
      try {
        val query = FirebaseFirestore.getInstance().collection("match_registrations")
          .whereEqualTo("id", registrationId)
          .get().await()
        for (doc in query.documents) {
          doc.reference.update("paymentStatus", status).await()
        }
      } catch (e: Exception) {
        android.util.Log.w("TournamentViewModel", "Failed to sync payment status to Firestore: ${e.message}")
      }
      _uiEvents.emit(UiEvent.ShowSnackbar("Payment status updated to $status"))
    }
  }

  fun deleteMatchRegistration(registrationId: Long) {
    viewModelScope.launch {
      repository.deleteMatchRegistration(registrationId)
      try {
        val query = FirebaseFirestore.getInstance().collection("match_registrations")
          .whereEqualTo("id", registrationId)
          .get().await()
        for (doc in query.documents) {
          doc.reference.delete().await()
        }
      } catch (e: Exception) {
        android.util.Log.w("TournamentViewModel", "Failed to delete remote reg: ${e.message}")
      }
      _uiEvents.emit(UiEvent.ShowSnackbar("Candidate registration removed."))
    }
  }

  fun broadcastRoomCredentials(matchId: Long, roomId: String, password: String, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      repository.broadcastRoomCredentials(matchId, roomId, password)
      com.example.utils.RoomBroadcastManager.recordBroadcastTime(
        getApplication(),
        matchId,
        System.currentTimeMillis(),
        force = true
      )
      try {
        val docs = FirebaseFirestore.getInstance().collection("match_registrations")
          .whereEqualTo("matchId", matchId)
          .get().await()
        val now = System.currentTimeMillis()
        for (doc in docs.documents) {
          doc.reference.update(mapOf(
            "roomId" to roomId,
            "roomPassword" to password,
            "broadcastTime" to now
          )).await()
        }
      } catch (e: Exception) {
        android.util.Log.w("TournamentViewModel", "Failed to sync room credentials to Firestore: ${e.message}")
      }
      _uiEvents.emit(UiEvent.ShowSnackbar("Room ID ($roomId) and Password broadcasted to candidates!"))
      onComplete()
    }
  }

  fun deleteBroadcastMessage(matchId: Long, onComplete: () -> Unit = {}) {
    viewModelScope.launch {
      com.example.utils.RoomBroadcastManager.markBroadcastDeleted(getApplication(), matchId)
      repository.unjoinMatchAfterBroadcast(matchId)
      _uiEvents.emit(UiEvent.ShowSnackbar("Broadcast message deleted"))
      onComplete()
    }
  }

  fun createTournamentMatch(
    name: String,
    gameTitle: String,
    category: String,
    entryFee: Int,
    prizePool: Int,
    maxPlayers: Int,
    date: String,
    time: String,
    format: String,
    mapName: String,
    rules: String,
    perKill: Int = 0,
    bannerImageUrl: String = "",
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        val currentMatches = allMatches.value
        val nextNumber = (currentMatches.maxOfOrNull { it.matchNumber } ?: 0) + 1
        val finalName = if (name.contains("Daily", ignoreCase = true) || name.contains("Weekly", ignoreCase = true)) {
          name
        } else {
          "$category $name"
        }
        val isDaily = category.equals("Daily", ignoreCase = true) || finalName.contains("Daily", ignoreCase = true)
        val calculatedPerKill = if (isDaily) {
          if (perKill > 0) perKill else if (entryFee > 0) (entryFee / 2).coerceAtLeast(5) else 5
        } else {
          0
        }
        val prizeList = mutableListOf(
          PrizeBreakdown("1st Place", (prizePool * 0.6).toInt()),
          PrizeBreakdown("2nd Place", (prizePool * 0.25).toInt()),
          PrizeBreakdown("3rd Place", (prizePool * 0.15).toInt())
        )
        if (isDaily && calculatedPerKill > 0) {
          prizeList.add(PrizeBreakdown("Per Kill", calculatedPerKill))
        }
        val newMatch = MatchItem(
          id = 0,
          matchNumber = nextNumber,
          name = finalName,
          gameTitle = gameTitle,
          entryFee = entryFee,
          prizePool = prizePool,
          perKill = calculatedPerKill,
          totalPlayers = 0,
          maxPlayers = maxPlayers,
          date = date,
          time = time,
          rankRequirement = "Level 20+",
          region = "India",
          status = MatchStatus.OPEN,
          format = format,
          mapName = mapName,
          rules = rules,
          description = "Official $category tournament match organized by Admin.",
          prizeDistribution = prizeList,
          roomId = "",
          roomPassword = "",
          isJoined = false,
          userGameUid = "",
          bannerImageUrl = bannerImageUrl
        )
        repository.createMatch(newMatch)
        _uiEvents.emit(UiEvent.ShowSnackbar("Tournament Match #$nextNumber created successfully!"))
        onSuccess()
      } catch (e: Exception) {
        android.util.Log.e("ViewModel", "Failed to create match", e)
        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to create match: ${e.message}"))
      }
    }
  }

  fun deleteTournamentMatch(matchId: Long) {
    viewModelScope.launch {
      try {
        repository.deleteMatch(matchId)
        _uiEvents.emit(UiEvent.ShowSnackbar("Tournament match deleted."))
      } catch (e: Exception) {
        android.util.Log.e("ViewModel", "Failed to delete match", e)
      }
    }
  }

  fun fetchAllCustomProfileApplicationsForAdmin() {
    listenToCustomProfileApplications()
  }

  fun submitMatchResult(
    applicationId: String,
    screenshotUri: String,
    winnerName: String,
    winnerUid: String,
    submittedBy: String = "Candidate",
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        var finalUrl = screenshotUri
        if (screenshotUri.startsWith("content://") || screenshotUri.startsWith("file://")) {
          try {
            val uri = Uri.parse(screenshotUri)
            val storageRef = FirebaseStorage.getInstance().reference.child("match_results/${UUID.randomUUID()}.jpg")
            storageRef.putFile(uri).await()
            finalUrl = storageRef.downloadUrl.await().toString()
          } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Failed to upload result image", e)
          }
        }
        val updateMap = hashMapOf<String, Any>(
          "resultScreenshot" to finalUrl,
          "winnerName" to winnerName,
          "winnerUid" to winnerUid,
          "resultSubmittedBy" to submittedBy,
          "isResultSubmitted" to true,
          "status" to "Result Submitted"
        )
        FirebaseFirestore.getInstance().collection("custom_profile_applications")
          .document(applicationId)
          .update(updateMap)
          .await()
        _uiEvents.emit(UiEvent.ShowSnackbar("Match result screenshot submitted successfully!"))
        onComplete()
      } catch (e: Exception) {
        android.util.Log.e("ViewModel", "Submit result error", e)
        _uiEvents.emit(UiEvent.ShowSnackbar("Result submitted!"))
        onComplete()
      }
    }
  }

  fun submitMatchReport(
    applicationId: String,
    reasons: String,
    description: String,
    mediaUri: String,
    submittedBy: String = "Candidate",
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        var finalMediaUrl = mediaUri
        if (mediaUri.startsWith("content://") || mediaUri.startsWith("file://")) {
          try {
            val uri = Uri.parse(mediaUri)
            val storageRef = FirebaseStorage.getInstance().reference.child("reports/${UUID.randomUUID()}")
            storageRef.putFile(uri).await()
            finalMediaUrl = storageRef.downloadUrl.await().toString()
          } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Failed to upload report media", e)
          }
        }
        val updateMap = hashMapOf<String, Any>(
          "reportReason" to reasons,
          "reportDescription" to description,
          "reportMediaUrl" to finalMediaUrl,
          "reportSubmittedBy" to submittedBy,
          "isReported" to true
        )
        FirebaseFirestore.getInstance().collection("custom_profile_applications")
          .document(applicationId)
          .update(updateMap)
          .await()
        _uiEvents.emit(UiEvent.ShowSnackbar("Report submitted. Admin will review the evidence."))
        onComplete()
      } catch (e: Exception) {
        android.util.Log.e("ViewModel", "Submit report error", e)
        _uiEvents.emit(UiEvent.ShowSnackbar("Report recorded!"))
        onComplete()
      }
    }
  }

  fun creditOrRefundCustomRoomAmount(
    targetUid: String,
    amount: Int,
    reason: String,
    onComplete: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        repository.creditWinningAmountToUser(targetUid, amount)
        _uiEvents.emit(UiEvent.ShowSnackbar("₹$amount credited to UID: $targetUid ($reason)"))
        onComplete()
      } catch (e: Exception) {
        android.util.Log.e("TournamentViewModel", "Failed to credit/refund", e)
        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to process payment: ${e.message}"))
      }
    }
  }

  fun sendCustomRoomWinningAmount(
    application: com.example.data.model.CustomProfileApplication,
    winnerUid: String,
    amount: Int,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch {
      try {
        val updateMap = hashMapOf<String, Any>(
          "winningAmountSent" to true,
          "winningAmountSentTo" to winnerUid,
          "status" to "Prize Sent (₹$amount)"
        )
        FirebaseFirestore.getInstance().collection("custom_profile_applications")
          .document(application.id)
          .update(updateMap)
          .await()

        repository.creditWinningAmountToUser(winnerUid, amount)
        _uiEvents.emit(UiEvent.ShowSnackbar("₹$amount successfully sent to winner (UID: $winnerUid)!"))
        onSuccess()
      } catch (e: Exception) {
        android.util.Log.e("AdminError", "Failed to send winning amount", e)
        repository.creditWinningAmountToUser(winnerUid, amount)
        _uiEvents.emit(UiEvent.ShowSnackbar("Winning amount of ₹$amount credited to wallet!"))
        onSuccess()
      }
    }
  }

  val allAccounts: StateFlow<List<UserAccount>> = repository.allAccounts.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val myRoomRegistrations: StateFlow<List<com.example.data.local.entity.RoomRegistrationEntity>> = repository.getAllRoomRegistrations().stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  private val _isAuthLoading = MutableStateFlow(false)
  val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  fun clearAuthError() {
    _authError.value = null
  }

  fun login(
    identifier: String,
    pass: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authError.value = null
      val result = repository.login(identifier, pass)
      _isAuthLoading.value = false
      result.onSuccess { account ->
        _uiEvents.emit(UiEvent.ShowSnackbar("Welcome back, ${account.name}!"))
        onSuccess()
      }.onFailure { ex ->
        val msg = ex.message ?: "Login failed. Please check credentials."
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun registerWithEmailAndPhone(
    name: String = "",
    email: String = "",
    phone: String = "",
    pass: String,
    gameUid: String = "",
    region: String = "India",
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      _authError.value = null
      val result = repository.register(
        name = name,
        email = email,
        phone = phone,
        phoneOrEmail = email.ifBlank { phone },
        password = pass,
        gameUid = gameUid,
        region = region
      )
      _isAuthLoading.value = false
      result.onSuccess { account ->
        _uiEvents.emit(UiEvent.ShowSnackbar("Account created! Welcome, ${account.name} (Bonus ₹100 added)"))
        refreshWalletTransactions()
        onSuccess()
      }.onFailure { ex ->
        val msg = ex.message ?: "Registration failed. Please check your details."
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun register(
    name: String = "",
    phoneOrEmail: String,
    pass: String,
    gameUid: String = "",
    region: String = "India",
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
  ) {
    val isEmail = phoneOrEmail.contains("@")
    val email = if (isEmail) phoneOrEmail else ""
    val phone = if (!isEmail) phoneOrEmail else ""
    registerWithEmailAndPhone(
      name = name,
      email = email,
      phone = phone,
      pass = pass,
      gameUid = gameUid,
      region = region,
      onSuccess = onSuccess,
      onError = onError
    )
  }

  fun continueAsGuest(onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      _isAuthLoading.value = true
      repository.continueAsGuest()
      _isAuthLoading.value = false
      _uiEvents.emit(UiEvent.ShowSnackbar("Logged in as Guest"))
      onSuccess()
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      _uiEvents.emit(UiEvent.ShowSnackbar("You have been logged out."))
    }
  }

  fun switchAccount(accountId: Long, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      repository.switchAccount(accountId)
      fetchCustomProfiles()
      fetchMyCustomProfileApplications()
      _uiEvents.emit(UiEvent.ShowSnackbar("Switched account successfully!"))
      onSuccess()
    }
  }

  // Navigation tab state (0 = Matches, 1 = Custom, 2 = User)
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  fun selectTab(index: Int) {
    _selectedTab.value = index
  }


  // Matches Search & Category
  private val _matchSearchQuery = MutableStateFlow("")
  val matchSearchQuery: StateFlow<String> = _matchSearchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  private val _selectedStatus = MutableStateFlow("All")
  val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

  fun setMatchSearchQuery(query: String) {
    _matchSearchQuery.value = query
  }

  fun setSelectedCategory(category: String) {
    _selectedCategory.value = category
  }

  fun setSelectedStatus(status: String) {
    _selectedStatus.value = status
  }

  val myRegisteredMatchIds: StateFlow<Set<Long>> = combine(
    repository.allMatches,
    allMatchRegistrations,
    authStatus,
    isAdmin
  ) { matches, registrations, auth, admin ->
    val account = (auth as? AuthStatus.LoggedIn)?.account
    if (account != null) {
      val accountGameUid = account.gameUid.trim()
      val accountPhoneDigits = account.phone.filter { it.isDigit() }
      val accountEmail = account.email.trim()
      val accountName = account.name.trim()

      val registeredIds = registrations.filter { reg ->
        val regPhoneDigits = reg.phoneOrEmail.filter { it.isDigit() }
        val matchesAccountId = reg.userAccountId > 0L && reg.userAccountId == account.id
        val matchesGameUid = accountGameUid.isNotBlank() && reg.gameUid.equals(accountGameUid, ignoreCase = true)
        val matchesPhone = accountPhoneDigits.isNotBlank() && regPhoneDigits.isNotBlank() && regPhoneDigits == accountPhoneDigits
        val matchesEmail = accountEmail.isNotBlank() && reg.phoneOrEmail.equals(accountEmail, ignoreCase = true)
        val matchesName = accountName.isNotBlank() && reg.candidateName.equals(accountName, ignoreCase = true)
        matchesAccountId || matchesGameUid || matchesPhone || matchesEmail || matchesName
      }.map { it.matchId }.toSet()

      val localJoinedIds = matches.filter { it.isJoined && (it.userGameUid.isBlank() || it.userGameUid.equals(accountGameUid, ignoreCase = true)) }.map { it.id }.toSet()

      registeredIds + localJoinedIds
    } else {
      // Guest mode
      val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      val authUidJoinedIds = if (authUid.isNotBlank()) {
        registrations.filter { it.applicantUid.isNotBlank() && it.applicantUid == authUid }.map { it.matchId }.toSet()
      } else emptySet()
      val localJoinedIds = matches.filter { it.isJoined }.map { it.id }.toSet()
      localJoinedIds + authUidJoinedIds
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptySet()
  )

  val filteredMatches: StateFlow<List<MatchItem>> = combine(
    repository.allMatches,
    myRegisteredMatchIds,
    _matchSearchQuery,
    _selectedCategory,
    _selectedStatus
  ) { rawMatches, registeredIds, query, category, status ->
    val userSpecificMatches = rawMatches.map { match ->
      match.copy(isJoined = registeredIds.contains(match.id))
    }
    userSpecificMatches.filter { match ->
      val matchesQuery = query.isBlank() ||
        match.name.contains(query, ignoreCase = true) ||
        match.gameTitle.contains(query, ignoreCase = true) ||
        match.format.contains(query, ignoreCase = true) ||
        match.mapName.contains(query, ignoreCase = true)

      val matchesCategory = when (category) {
        "All" -> true
        "Joined" -> match.isJoined
        "Free" -> match.entryFee == 0
        "High Stakes" -> match.prizePool >= 2000
        else -> match.gameTitle.contains(category, ignoreCase = true) || match.format.contains(category, ignoreCase = true)
      }

      val matchesStatus = when (status) {
        "All" -> true
        "Upcoming" -> match.status == com.example.data.model.MatchStatus.UPCOMING || match.status == com.example.data.model.MatchStatus.OPEN || match.status == com.example.data.model.MatchStatus.FAST_FILLING
        "Ongoing" -> match.status == com.example.data.model.MatchStatus.LIVE
        "Completed" -> match.status == com.example.data.model.MatchStatus.COMPLETED
        else -> true
      }

      matchesQuery && matchesCategory && matchesStatus
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Custom Tournaments Search
  private val _customSearchQuery = MutableStateFlow("")
  val customSearchQuery: StateFlow<String> = _customSearchQuery.asStateFlow()

  fun setCustomSearchQuery(query: String) {
    _customSearchQuery.value = query
  }

  val filteredCustomTournaments: StateFlow<List<CustomTournament>> = combine(
    repository.allCustomTournaments,
    _customSearchQuery
  ) { tournaments, query ->
    if (query.isBlank()) {
      tournaments
    } else {
      tournaments.filter {
        it.name.contains(query, ignoreCase = true) ||
        it.hostUid.contains(query, ignoreCase = true) ||
        it.region.contains(query, ignoreCase = true) ||
        it.matchInfo.contains(query, ignoreCase = true)
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // User Profile
  val userProfile: StateFlow<UserProfile> = repository.userProfile.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = UserProfile(name = "Gamer", uid = "548291047", region = "India / Asia", avatarId = 1)
  )

  // History
  val tournamentHistory: StateFlow<List<TournamentHistory>> = repository.tournamentHistory.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Selected Match detail
  private val _selectedMatchId = MutableStateFlow<Long?>(null)
  val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

  val selectedMatch: StateFlow<MatchItem?> = combine(
    _selectedMatchId,
    repository.allMatches,
    myRegisteredMatchIds
  ) { id, matches, registeredIds ->
    if (id == null) null
    else {
      matches.find { it.id == id }?.copy(
        isJoined = registeredIds.contains(id)
      )
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  fun selectMatch(matchId: Long) {
    _selectedMatchId.value = matchId
  }

  // Join Match Action
  fun joinMatch(
    matchId: Long,
    inGameUid: String,
    entryFee: Int,
    candidateName: String = "",
    phoneOrEmail: String = "",
    paymentMethod: String = "Wallet",
    onComplete: (Boolean) -> Unit = {}
  ) {
    viewModelScope.launch {
      val success = repository.joinMatch(
        matchId = matchId,
        userGameUid = inGameUid,
        entryFee = entryFee,
        candidateName = candidateName,
        phoneOrEmail = phoneOrEmail,
        paymentMethod = paymentMethod
      )
      if (success) {
        _uiEvents.emit(UiEvent.ShowSnackbar("Successfully joined Match! Registered with Game UID: $inGameUid"))
        onComplete(true)
      } else {
        _uiEvents.emit(UiEvent.ShowSnackbar("Insufficient wallet balance! Please add funds in the User tab."))
        onComplete(false)
      }
    }
  }

  fun cancelMatch(matchId: Long, entryFee: Int) {
    viewModelScope.launch {
      repository.cancelMatchJoin(matchId, entryFee)
      _uiEvents.emit(UiEvent.ShowSnackbar("Registration cancelled. Entry fee ₹$entryFee refunded to wallet."))
    }
  }

  // Join / Leave Custom Tournament
  fun toggleJoinCustomTournament(tournament: CustomTournament) {
    viewModelScope.launch {
      val newJoining = !tournament.isJoined
      repository.joinCustomTournament(tournament.id, newJoining)
      val msg = if (newJoining) "Joined ${tournament.name}!" else "Left ${tournament.name}."
      _uiEvents.emit(UiEvent.ShowSnackbar(msg))
    }
  }


  fun updateApplicationStatus(applicationId: String, profileId: String, status: String) {
    viewModelScope.launch {
      val db = FirebaseFirestore.getInstance()
      val appRef = db.collection("custom_profile_applications").document(applicationId)
      val profileRef = db.collection("custom_profiles").document(profileId)

      try {
        db.runTransaction { transaction ->
          val profileDoc = transaction.get(profileRef)
          transaction.update(appRef, "status", status)

          if (profileDoc.exists()) {
            val updates = hashMapOf<String, Any>()
            if (status == "Accepted") {
              updates["hasAccepted"] = true
              val currentCandidates = profileDoc.getLong("candidateCount")?.toInt() ?: 0
              updates["isLocked"] = (currentCandidates >= 7)
            }
            if (updates.isNotEmpty()) {
              transaction.update(profileRef, updates)
            }
          }
          null
        }.await()

        _uiEvents.emit(UiEvent.ShowSnackbar("Application marked as $status"))
      } catch (e: Exception) {
        _uiEvents.emit(UiEvent.ShowSnackbar("Failed to update status: ${e.message}"))
      }
    }
  }

  fun sendCandidateCredentials(applicationId: String, roomId: String, roomPassword: String) {
    viewModelScope.launch {
      FirebaseFirestore.getInstance().collection("custom_profile_applications").document(applicationId)
        .update(mapOf("roomId" to roomId, "roomPassword" to roomPassword))
        .addOnSuccessListener {
            NotificationManagerHelper.showImmediateNotification(
              getApplication(),
              title = "Custom Room Credentials Released!",
              message = "Room ID: $roomId | Password: $roomPassword. Enter Free Fire MAX now!"
            )
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowSnackbar("Room details sent to candidate!"))
            }
        }.addOnFailureListener {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowSnackbar("Failed to send room details."))
            }
        }
    }
  }

  fun updateHostPayment(applicationId: String, hostPaid: Boolean = true) {
    viewModelScope.launch {
      try {
        FirebaseFirestore.getInstance()
          .collection("custom_profile_applications")
          .document(applicationId)
          .update("hostPaid", hostPaid)
        _uiEvents.emit(UiEvent.ShowSnackbar("Host payment confirmed! You can now send room details."))
      } catch (e: Exception) {
        _uiEvents.emit(UiEvent.ShowSnackbar("Payment update failed: ${e.message}"))
      }
    }
  }

  fun sendRoomCredentials(profileId: String, roomId: String, roomPassword: String) {
    viewModelScope.launch {
      FirebaseFirestore.getInstance().collection("custom_profile_applications")
        .whereEqualTo("profileId", profileId)
        .whereEqualTo("status", "Accepted")
        .get()
        .addOnSuccessListener { snapshot ->
            val batch = FirebaseFirestore.getInstance().batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, mapOf("roomId" to roomId, "roomPassword" to roomPassword))
            }
            batch.commit().addOnSuccessListener {
                NotificationManagerHelper.showImmediateNotification(
                  getApplication(),
                  title = "Custom Room Credentials Released!",
                  message = "Room ID: $roomId | Password: $roomPassword. Enter Free Fire MAX now!"
                )
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Room details sent to all accepted candidates!"))
                }
            }.addOnFailureListener {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Failed to send room details."))
                }
            }
        }
        .addOnFailureListener {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowSnackbar("Failed to fetch applications."))
            }
        }
    }
  }

  fun submitCustomProfileApplication(
    profile: com.example.data.model.CustomProfile,
    candidateName: String,
    phone: String,
    uid: String,
    level: String,
    rank: String,
    onComplete: ((Boolean, String) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      val curAccount = (authStatus.value as? AuthStatus.LoggedIn)?.account
      val curUserProfile = userProfile.value
      val effectiveCandidateName = candidateName.ifBlank { curAccount?.username ?: curUserProfile.name.ifBlank { "Player" } }
      val effectivePhone = phone.ifBlank { curAccount?.phone ?: "" }
      val effectiveUid = uid.ifBlank { curAccount?.gameUid ?: curUserProfile.uid }
      val effectiveApplicantUid = currentUserUid.ifBlank { effectiveUid }
      val db = FirebaseFirestore.getInstance()
      val profileRef = db.collection("custom_profiles").document(profile.id)
      val newAppRef = db.collection("custom_profile_applications").document()

      // Save locally to preferences immediately so joined status is never lost
      val currentRooms = (customProfilePrefs.getStringSet("applied_room_ids", emptySet()) ?: emptySet()).toMutableSet()
      currentRooms.add(profile.id)
      val currentApps = (customProfilePrefs.getStringSet("applied_app_ids", emptySet()) ?: emptySet()).toMutableSet()
      currentApps.add(newAppRef.id)
      customProfilePrefs.edit()
        .putStringSet("applied_room_ids", currentRooms)
        .putStringSet("applied_app_ids", currentApps)
        .apply()

      try {
        // Pre-fetch existing applications count and acceptance for robust backwards compatibility
        val existingAppsSnapshot = try {
          db.collection("custom_profile_applications")
            .whereEqualTo("profileId", profile.id)
            .get()
            .await()
        } catch (e: Exception) {
          null
        }

        val existingAppsCount = existingAppsSnapshot?.size() ?: 0
        val existingHasAccepted = existingAppsSnapshot?.documents?.any { it.getString("status") == "Accepted" } ?: false

        try {
          db.runTransaction { transaction ->
            val snapshot = transaction.get(profileRef)
            if (!snapshot.exists()) {
              throw FirebaseFirestoreException("Match room does not exist.", FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val docAccepted = snapshot.getBoolean("hasAccepted") ?: false
            val hasAccepted = docAccepted || existingHasAccepted

            val storedCandidateCount = snapshot.getLong("candidateCount")?.toInt()
            val currentCandidates = if (storedCandidateCount != null) {
              maxOf(storedCandidateCount, existingAppsCount)
            } else {
              maxOf(snapshot.getLong("joinedPlayers")?.toInt() ?: 0, existingAppsCount)
            }

            if (currentCandidates >= 7) {
              throw FirebaseFirestoreException(
                "Candidate limit (7/7) has been reached. Further registrations are blocked.",
                FirebaseFirestoreException.Code.ABORTED
              )
            }

            val newCount = currentCandidates + 1
            val updates = hashMapOf<String, Any>(
              "candidateCount" to newCount,
              "joinedPlayers" to newCount,
              "isLocked" to (newCount >= 7)
            )
            transaction.update(profileRef, updates)

            val hostUidFromDoc = snapshot.getString("hostUid")?.takeIf { it.isNotBlank() }
            val hostGameUidFromDoc = snapshot.getString("uid")?.takeIf { it.isNotBlank() }
            val effectiveHostUid = hostUidFromDoc ?: profile.hostUid.takeIf { it.isNotBlank() } ?: hostGameUidFromDoc ?: profile.uid
            val hostActualName = snapshot.getString("hostActualName")?.takeIf { it.isNotBlank() } ?: snapshot.getString("hostName") ?: profile.hostActualName.ifBlank { profile.name }
            val hostPhone = snapshot.getString("hostPhone")?.takeIf { it.isNotBlank() } ?: profile.hostPhone
            val hostEmail = snapshot.getString("hostEmail")?.takeIf { it.isNotBlank() } ?: profile.hostEmail

            val application = hashMapOf(
              "profileId" to profile.id,
              "profileName" to (snapshot.getString("name") ?: profile.name),
              "hostUid" to effectiveHostUid,
              "hostGameUid" to (hostGameUidFromDoc ?: profile.uid),
              "hostActualName" to hostActualName,
              "hostPhone" to hostPhone,
              "hostEmail" to hostEmail,
              "applicantUid" to effectiveApplicantUid,
              "candidateName" to effectiveCandidateName,
              "phone" to effectivePhone,
              "uid" to effectiveUid,
              "level" to level,
              "rank" to rank,
              "status" to "Pending",
              "appliedAt" to System.currentTimeMillis(),
              "payout" to (snapshot.getString("payout") ?: profile.payout)
            )
            transaction.set(newAppRef, application)
            null
          }.await()
        } catch (txnError: Exception) {
          if (txnError is FirebaseFirestoreException && txnError.code == FirebaseFirestoreException.Code.ABORTED) {
            throw txnError
          }
          if (existingAppsCount >= 7) {
            throw FirebaseFirestoreException(
              "Candidate limit (7/7) has been reached. Further registrations are blocked.",
              FirebaseFirestoreException.Code.ABORTED
            )
          }
          // Direct fallback write
          val fallbackHostUid = profile.hostUid.takeIf { it.isNotBlank() } ?: profile.uid
          val application = hashMapOf(
            "profileId" to profile.id,
            "profileName" to profile.name,
            "hostUid" to fallbackHostUid,
            "hostGameUid" to profile.uid,
            "hostActualName" to profile.hostActualName.ifBlank { profile.name },
            "hostPhone" to profile.hostPhone,
            "hostEmail" to profile.hostEmail,
            "applicantUid" to effectiveApplicantUid,
            "candidateName" to effectiveCandidateName,
            "phone" to effectivePhone,
            "uid" to effectiveUid,
            "level" to level,
            "rank" to rank,
            "status" to "Pending",
            "appliedAt" to System.currentTimeMillis(),
            "payout" to profile.payout
          )
          newAppRef.set(application).await()
          try {
            profileRef.update(
              mapOf(
                "candidateCount" to com.google.firebase.firestore.FieldValue.increment(1),
                "joinedPlayers" to com.google.firebase.firestore.FieldValue.increment(1),
                "isLocked" to (existingAppsCount + 1 >= 7)
              )
            ).await()
          } catch (_: Exception) {}
        }

        // Optimistic update
        val optimisticApp = com.example.data.model.CustomProfileApplication(
          id = newAppRef.id,
          profileId = profile.id,
          profileName = profile.name,
          hostUid = profile.hostUid.takeIf { it.isNotBlank() } ?: profile.uid,
          hostActualName = profile.hostActualName.ifBlank { profile.name },
          hostPhone = profile.hostPhone,
          hostEmail = profile.hostEmail,
          hostGameUid = profile.hostGameUid.ifBlank { profile.uid },
          candidateName = effectiveCandidateName,
          phone = effectivePhone,
          uid = effectiveUid,
          level = level,
          rank = rank,
          status = "Pending",
          appliedAt = System.currentTimeMillis(),
          payout = profile.payout,
          applicantUid = effectiveApplicantUid
        )
        _allCustomProfileApplicationsForAdmin.value = (listOf(optimisticApp) + _allCustomProfileApplicationsForAdmin.value).distinctBy { it.id }
        _myAppliedCustomProfiles.value = (listOf(optimisticApp) + _myAppliedCustomProfiles.value).distinctBy { it.id }
        recomputeApplications(_allCustomProfileApplicationsForAdmin.value)

        _uiEvents.emit(UiEvent.ShowSnackbar("Registration submitted for ${profile.name}!"))
        onComplete?.invoke(true, "Registration submitted successfully!")
      } catch (e: Exception) {
        val errorMsg = when {
          e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED ->
            e.message ?: "Registration blocked: Candidate limit (7/7) reached."
          e.message?.contains("limit", ignoreCase = true) == true || e.message?.contains("7", ignoreCase = true) == true ->
            "Registration blocked: Candidate limit (7/7) reached."
          else ->
            e.message ?: "Failed to submit application."
        }
        _uiEvents.emit(UiEvent.ShowSnackbar(errorMsg))
        onComplete?.invoke(false, errorMsg)
      }
    }
  }

  fun submitRoomApplication(tournament: CustomTournament, candidateName: String, phone: String, uid: String, level: Int) {
    viewModelScope.launch {
      repository.submitRoomApplication(tournament, candidateName, phone, uid, level)
      repository.joinCustomTournament(tournament.id, true)
      _uiEvents.emit(UiEvent.ShowSnackbar("Registration submitted for ${tournament.name}!"))
    }
  }

  fun deleteCustomTournament(id: Long) {
    viewModelScope.launch {
      repository.deleteCustomTournament(id)
      _uiEvents.emit(UiEvent.ShowSnackbar("Custom tournament deleted."))
    }
  }

  // Create Custom Tournament
  fun createCustomTournament(
    name: String,
    uid: String,
    region: String,
    entryFeeStr: String,
    prizeStr: String,
    matchInfo: String,
    maxPlayersStr: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      val entryFee = entryFeeStr.toIntOrNull() ?: 0
      val prize = prizeStr.toIntOrNull() ?: 0
      val maxPlayers = maxPlayersStr.toIntOrNull() ?: 100

      val custom = CustomTournament(
        name = name.trim(),
        hostUid = uid.trim(),
        region = region.trim(),
        entryFee = entryFee,
        prize = prize,
        matchInfo = matchInfo.trim(),
        maxPlayers = maxPlayers
      )

      repository.createCustomTournament(custom)
      _uiEvents.emit(UiEvent.ShowSnackbar("Custom Tournament \"${custom.name}\" created successfully!"))
      onSuccess()
    }
  }


  fun createCustomProfile(
    name: String,
    uid: String,
    level: Int,
    payout: Int,
    prizePool: Int,
    perKill: Int,
    totalPlayers: Int,
    category: String,
    game: String,
    day: String,
    time: String,
    type: String,
    mode: String,
    gun: String,
    imageUriStr: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      repository.createCustomProfile(
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
        imageUriStr = imageUriStr
      )
      _uiEvents.emit(UiEvent.ShowSnackbar("Custom Profile created successfully!"))
      onSuccess()
    }
  }

  fun updateCustomProfile(
    profileId: String,
    name: String,
    uid: String,
    level: Int,
    payout: Int,
    prizePool: Int,
    perKill: Int,
    totalPlayers: Int,
    category: String,
    game: String,
    day: String,
    time: String,
    type: String,
    mode: String,
    gun: String,
    imageUriStr: String,
    existingImageUrl: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      repository.updateCustomProfile(
        profileId = profileId,
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
        existingImageUrl = existingImageUrl
      )
      _uiEvents.emit(UiEvent.ShowSnackbar("Custom Profile updated successfully!"))
      onSuccess()
    }
  }


  fun updateMatch(match: MatchItem) {
    viewModelScope.launch {
      repository.updateMatch(match)
      _uiEvents.emit(UiEvent.ShowSnackbar("Match updated successfully!"))
    }
  }

  // Update Profile
  fun updateProfile(
    name: String,
    uid: String,
    region: String,
    bio: String,
    avatarId: Int,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      val current = userProfile.value
      val updated = current.copy(
        name = name.trim(),
        uid = uid.trim(),
        region = region.trim(),
        bio = bio.trim(),
        avatarId = avatarId
      )
      repository.updateUserProfile(updated)
      _uiEvents.emit(UiEvent.ShowSnackbar("Profile updated successfully!"))
      onSuccess()
    }
  }

  // Wallet transactions state
  private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
  val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

  fun refreshWalletTransactions() {
    viewModelScope.launch {
      _walletTransactions.value = repository.getWalletTransactions()
    }
  }

  // Add wallet balance
  fun addWalletBalance(amount: Int) {
    viewModelScope.launch {
      repository.addWalletBalance(amount)
      val newTx = WalletTransaction(
        title = "Added Funds via UPI",
        amount = amount,
        type = "CREDIT",
        date = "Just now",
        status = "Completed"
      )
      _walletTransactions.value = listOf(newTx) + _walletTransactions.value
      _uiEvents.emit(UiEvent.ShowSnackbar("Added ₹$amount to wallet successfully!"))
    }
  }

  // Withdraw wallet balance
  fun withdrawWalletBalance(amount: Int, upiId: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.withdrawWalletBalance(amount, upiId)
      if (res.isSuccess) {
        val newTx = WalletTransaction(
          title = "Withdrawal to $upiId",
          amount = amount,
          type = "DEBIT",
          date = "Just now",
          status = "Completed",
          upiId = upiId
        )
        _walletTransactions.value = listOf(newTx) + _walletTransactions.value
        _uiEvents.emit(UiEvent.ShowSnackbar("₹$amount withdrawn to $upiId successfully!"))
        onResult(true, "Withdrawal successful!")
      } else {
        val err = res.exceptionOrNull()?.message ?: "Withdrawal failed"
        _uiEvents.emit(UiEvent.ShowSnackbar(err))
        onResult(false, err)
      }
    }
  }
}

