package com.example.data.repository

import com.example.data.local.dao.ActiveSessionDao
import com.example.data.local.dao.CustomTournamentDao
import com.example.data.local.dao.MatchDao
import com.example.data.local.dao.MatchRegistrationDao
import com.example.data.local.dao.TournamentHistoryDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.ActiveSessionEntity
import com.example.data.local.entity.CustomTournamentEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.MatchRegistrationEntity
import com.example.data.local.entity.TournamentHistoryEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.AuthStatus
import com.example.data.model.CustomProfile
import com.example.data.model.CustomTournament
import com.example.data.model.MatchItem
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.PrizeBreakdown
import com.example.data.model.TournamentHistory
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map


import com.google.firebase.auth.FirebaseAuth

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import android.util.Log



class TournamentRepository(
  private val matchDao: MatchDao,
  private val customTournamentDao: CustomTournamentDao,
  private val userProfileDao: UserProfileDao,
  private val historyDao: TournamentHistoryDao,
  private val userAccountDao: UserAccountDao,
  private val activeSessionDao: ActiveSessionDao,
  private val roomRegistrationDao: com.example.data.local.dao.RoomRegistrationDao,
  private val matchRegistrationDao: com.example.data.local.dao.MatchRegistrationDao
) {

  val allMatches: Flow<List<MatchItem>> = matchDao.getAllMatches().map { entities ->
    entities.map { it.toDomain() }
  }

  val joinedMatches: Flow<List<MatchItem>> = matchDao.getJoinedMatches().map { entities ->
    entities.map { it.toDomain() }
  }

  fun getMatchByIdFlow(id: Long): Flow<MatchItem?> = matchDao.getMatchByIdFlow(id).map { it?.toDomain() }

  suspend fun getMatchById(id: Long): MatchItem? = matchDao.getMatchById(id)?.toDomain()

  suspend fun createMatch(match: MatchItem): Long {
    val entity = MatchEntity(
      id = 0,
      matchNumber = match.matchNumber,
      name = match.name,
      gameTitle = match.gameTitle,
      entryFee = match.entryFee,
      prizePool = match.prizePool,
      perKill = match.perKill,
      totalPlayers = match.totalPlayers,
      maxPlayers = match.maxPlayers,
      date = match.date,
      time = match.time,
      rankRequirement = match.rankRequirement,
      region = match.region,
      status = match.status.name,
      format = match.format,
      mapName = match.mapName,
      rules = match.rules,
      description = match.description,
      prizeDistributionJson = "[]",
      roomId = match.roomId,
      roomPassword = match.roomPassword,
      isJoined = false,
      userGameUid = "",
      bannerImageUrl = match.bannerImageUrl,
      isRoomBroadcasted = match.isRoomBroadcasted
    )
    val localId = matchDao.insertMatch(entity)

    try {
      val matchMap = hashMapOf<String, Any>(
        "id" to localId,
        "matchNumber" to match.matchNumber,
        "name" to match.name,
        "gameTitle" to match.gameTitle,
        "entryFee" to match.entryFee,
        "prizePool" to match.prizePool,
        "perKill" to match.perKill,
        "totalPlayers" to match.totalPlayers,
        "maxPlayers" to match.maxPlayers,
        "date" to match.date,
        "time" to match.time,
        "rankRequirement" to match.rankRequirement,
        "region" to match.region,
        "status" to match.status.name,
        "format" to match.format,
        "mapName" to match.mapName,
        "rules" to match.rules,
        "description" to match.description,
        "prizeDistributionJson" to "[]",
        "roomId" to match.roomId,
        "roomPassword" to match.roomPassword,
        "bannerImageUrl" to match.bannerImageUrl,
        "isRoomBroadcasted" to match.isRoomBroadcasted,
        "createdAt" to System.currentTimeMillis()
      )
      firestore.collection("matches").document(localId.toString()).set(matchMap).await()
      // Remove any historical tombstone for this matchId
      firestore.collection("deleted_matches").document(localId.toString()).delete()
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to sync created match to Firestore: ${e.message}")
    }
    return localId
  }

  suspend fun deleteMatch(matchId: Long) {
    // 1. Delete from local SQLite
    matchDao.deleteMatch(matchId)
    matchRegistrationDao.deleteRegistrationsForMatch(matchId)

    // 2. Delete from Firestore and record permanent tombstone so it never reappears
    try {
      firestore.collection("matches").document(matchId.toString()).delete().await()
      val tombstone = mapOf(
        "matchId" to matchId,
        "deletedAt" to System.currentTimeMillis()
      )
      firestore.collection("deleted_matches").document(matchId.toString()).set(tombstone).await()
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to delete match from Firestore: ${e.message}")
    }
  }

  suspend fun updateMatch(match: MatchItem) {
    val entity = matchDao.getMatchById(match.id)
    if (entity != null) {
      val prizeJson = if (match.prizeDistribution.isNotEmpty()) {
        match.prizeDistribution.joinToString(" | ") { "${it.rank}: ₹${it.amount}" }
      } else entity.prizeDistributionJson

      val updated = entity.copy(
        name = match.name,
        gameTitle = match.gameTitle,
        entryFee = match.entryFee,
        prizePool = match.prizePool,
        perKill = match.perKill,
        maxPlayers = match.maxPlayers,
        date = match.date,
        time = match.time,
        rankRequirement = match.rankRequirement,
        rules = match.rules,
        description = match.description,
        prizeDistributionJson = prizeJson,
        format = match.format,
        mapName = match.mapName,
        status = match.status.name,
        roomId = match.roomId,
        roomPassword = match.roomPassword,
        bannerImageUrl = match.bannerImageUrl,
        isRoomBroadcasted = match.isRoomBroadcasted
      )
      matchDao.updateMatch(updated)

      try {
        val matchMap = hashMapOf<String, Any>(
          "id" to updated.id,
          "matchNumber" to updated.matchNumber,
          "name" to updated.name,
          "gameTitle" to updated.gameTitle,
          "entryFee" to updated.entryFee,
          "prizePool" to updated.prizePool,
          "perKill" to updated.perKill,
          "totalPlayers" to updated.totalPlayers,
          "maxPlayers" to updated.maxPlayers,
          "date" to updated.date,
          "time" to updated.time,
          "rankRequirement" to updated.rankRequirement,
          "region" to updated.region,
          "status" to updated.status,
          "format" to updated.format,
          "mapName" to updated.mapName,
          "rules" to updated.rules,
          "description" to updated.description,
          "prizeDistributionJson" to updated.prizeDistributionJson,
          "roomId" to updated.roomId,
          "roomPassword" to updated.roomPassword,
          "bannerImageUrl" to updated.bannerImageUrl,
          "isRoomBroadcasted" to updated.isRoomBroadcasted,
          "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("matches").document(match.id.toString()).set(matchMap, SetOptions.merge()).await()
      } catch (e: Exception) {
        Log.e("TournamentRepo", "Failed to sync updated match to Firestore: ${e.message}")
      }
    }
  }

  val allMatchRegistrations: Flow<List<MatchRegistration>> = matchRegistrationDao.getAllMatchRegistrations().map { list ->
    list.map { it.toMatchRegistration() }
  }

  fun getRegistrationsForMatch(matchId: Long): Flow<List<MatchRegistration>> = matchRegistrationDao.getRegistrationsForMatch(matchId).map { list ->
    list.map { it.toMatchRegistration() }
  }

  suspend fun updateMatchRegistrationPaymentStatus(registrationId: Long, status: String) {
    matchRegistrationDao.updatePaymentStatus(registrationId, status)
  }

  suspend fun deleteMatchRegistration(registrationId: Long) {
    matchRegistrationDao.deleteRegistration(registrationId)
  }

  suspend fun purgeDummyRegistrations() {
    try {
      matchRegistrationDao.purgeDummyRegistrations()
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to purge dummy registrations", e)
    }
  }

  suspend fun syncRemoteMatchRegistrations(remoteList: List<MatchRegistration>) {
    try {
      val dummyNames = setOf("ThunderStrike", "ShadowViper", "PhoenixFire", "AlphaWolf", "GhostRider")
      for (remote in remoteList) {
        if (remote.candidateName in dummyNames || remote.matchId <= 0L) continue
        val entity = MatchRegistrationEntity(
          id = remote.id,
          matchId = remote.matchId,
          matchTitle = remote.matchTitle,
          candidateName = remote.candidateName,
          gameUid = remote.gameUid,
          phoneOrEmail = remote.phoneOrEmail,
          paymentStatus = remote.paymentStatus,
          entryFee = remote.entryFee,
          registeredAt = remote.registeredAt,
          roomId = remote.roomId,
          roomPassword = remote.roomPassword,
          applicantUid = remote.applicantUid,
          userAccountId = remote.userAccountId
        )
        matchRegistrationDao.insertRegistration(entity)
      }
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Error syncing remote registrations", e)
    }
  }

  suspend fun broadcastRoomCredentials(matchId: Long, roomId: String, password: String) {
    val entity = matchDao.getMatchById(matchId)
    if (entity != null) {
      matchDao.updateMatch(entity.copy(roomId = roomId, roomPassword = password, status = "LIVE", isRoomBroadcasted = true))
    }
    matchRegistrationDao.broadcastRoomCredentials(matchId, roomId, password)
    try {
      val updateMap = mapOf(
        "roomId" to roomId,
        "roomPassword" to password,
        "status" to "LIVE",
        "isRoomBroadcasted" to true,
        "broadcastedAt" to System.currentTimeMillis()
      )
      firestore.collection("matches").document(matchId.toString()).set(updateMap, SetOptions.merge()).await()
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to sync room broadcast to Firestore: ${e.message}")
    }
  }


  val allCustomTournaments: Flow<List<CustomTournament>> = customTournamentDao.getAllCustomTournaments().map { entities ->
    entities.map { it.toDomain() }
  }

  val userProfile: Flow<UserProfile> = userProfileDao.getUserProfileFlow().map {
    it?.toDomain() ?: UserProfile(
      name = "Gamer",
      uid = "10000001",
      region = "India",
      avatarId = 0
    )
  }

  val authStatus: Flow<AuthStatus> = activeSessionDao.getActiveSessionFlow().flatMapLatest { session ->
    if (session == null || !session.isLoggedIn) {
      flowOf(AuthStatus.LoggedOut)
    } else if (session.isGuest) {
      val guestAccount = UserAccount(
        id = -1,
        username = "guest_player",
        email = "",
        phone = "",
        name = "Guest Player",
        gameUid = "99999999",
        region = "Global",
        walletBalance = 0,
        rank = "Bronze I",
        rankPoints = 1000,
        bio = "Exploring esports tournaments in Guest Mode"
      )
      flowOf(AuthStatus.LoggedIn(guestAccount, isGuest = true))
    } else {
      userAccountDao.findByIdFlow(session.activeAccountId).map { accountEntity ->
        if (accountEntity != null) {
          AuthStatus.LoggedIn(accountEntity.toDomain(), isGuest = false)
        } else {
          AuthStatus.LoggedOut
        }
      }
    }
  }

  val allAccounts: Flow<List<UserAccount>> = userAccountDao.getAllAccounts().map { list ->
    list.map { it.toDomain() }
  }


  private val auth = FirebaseAuth.getInstance()
  private val firestore = FirebaseFirestore.getInstance()

  private fun getEmailForAuth(input: String): String {
    val isEmail = input.contains("@") && input.contains(".")
    val digitsOnly = input.filter { it.isDigit() }
    val isPhone = !isEmail && digitsOnly.length >= 7
    return if (isEmail) {
      input.trim().lowercase()
    } else if (isPhone) {
      "${digitsOnly}@tourneymatch.com"
    } else {
      input.trim()
    }
  }

  suspend fun ensureAdminAccount(): UserAccountEntity {
    val adminPhone = "6205964987"
    var account = userAccountDao.findByPhone(adminPhone)
      ?: userAccountDao.findByIdentifier(adminPhone)
      ?: userAccountDao.findByEmail("${adminPhone}@tourneymatch.com")
      ?: userAccountDao.findByUsername("admin_6205964987")
      ?: userAccountDao.findByUsername("admin")
      ?: userAccountDao.findByUsername("admine")
    if (account == null) {
      account = UserAccountEntity(
        id = 0,
        username = "admin_6205964987",
        email = "${adminPhone}@tourneymatch.com",
        phone = adminPhone,
        password = "112233",
        name = "Admin (6205964987)",
        gameUid = adminPhone,
        region = "India",
        avatarId = 0,
        walletBalance = 50000,
        totalEarnings = 100000,
        matchesJoined = 0,
        matchesWon = 0,
        rank = "Tournament Admin",
        rankPoints = 9999,
        bio = "Official Tournament Administrator & Payout Manager",
        createdAt = System.currentTimeMillis()
      )
      val insertedId = userAccountDao.insertAccount(account)
      account = account.copy(id = insertedId)
    } else if (account.phone == adminPhone && account.password != "112233") {
      account = account.copy(password = "112233")
      userAccountDao.updateAccount(account)
    }
    return account
  }

  suspend fun ensurePlayerAccountForUser(uid: String): UserAccountEntity {
    var account = userAccountDao.findByGameUid(uid)
      ?: userAccountDao.findByEmail("${uid}@tourneymatch.com")
      ?: userAccountDao.findByUsername("player_${uid.take(8)}")
    if (account == null || account.id == 2L || account.phone.contains("6205964987")) {
      val defaultPlayer = userAccountDao.findById(1L)
      if (defaultPlayer != null && defaultPlayer.id != 2L && !defaultPlayer.phone.contains("6205964987")) {
        account = defaultPlayer.copy(gameUid = uid)
        userAccountDao.updateAccount(account)
      } else {
        val newPlayer = UserAccountEntity(
          id = 0,
          username = "player_${uid.take(8)}",
          email = "${uid}@tourneymatch.com",
          phone = "",
          password = "password123",
          name = "Player",
          gameUid = uid,
          region = "India",
          avatarId = 1,
          walletBalance = 850,
          totalEarnings = 0,
          matchesJoined = 0,
          matchesWon = 0,
          rank = "Level 65",
          rankPoints = 3280,
          bio = "Competitive Free Fire player",
          createdAt = System.currentTimeMillis()
        )
        val newId = userAccountDao.insertAccount(newPlayer)
        account = newPlayer.copy(id = newId)
      }
    }
    return account
  }

  suspend fun syncSessionForFirebaseUser(firebaseUid: String) {
    if (firebaseUid == com.example.ui.viewmodel.AdminConfig.NON_ADMIN_UID || com.example.ui.viewmodel.AdminConfig.NON_ADMIN_UIDS.contains(firebaseUid)) {
      // Force separation from admin account for non-admin user
      val session = activeSessionDao.getActiveSession()
      val curAcc = session?.let { userAccountDao.findById(it.activeAccountId) }
      if (curAcc == null || curAcc.id == 2L || curAcc.phone.contains("6205964987") || curAcc.username.contains("admin", ignoreCase = true)) {
        val playerAccount = ensurePlayerAccountForUser(firebaseUid)
        activeSessionDao.setActiveSession(
          com.example.data.local.entity.ActiveSessionEntity(
            id = 1,
            activeAccountId = playerAccount.id,
            isLoggedIn = true,
            isGuest = false,
            lastLoginTime = System.currentTimeMillis()
          )
        )
        syncProfileWithAccount(playerAccount)
      }
    } else if (firebaseUid == com.example.ui.viewmodel.AdminConfig.ADMIN_UID || com.example.ui.viewmodel.AdminConfig.ADMIN_UIDS.contains(firebaseUid)) {
      val adminAccount = ensureAdminAccount()
      activeSessionDao.setActiveSession(
        com.example.data.local.entity.ActiveSessionEntity(
          id = 1,
          activeAccountId = adminAccount.id,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )
      syncProfileWithAccount(adminAccount)
    }
  }

  suspend fun saveAdminAccountToFirebaseBackend() {
    try {
      // Guarantee local SQLite/Room admin account is initialized
      ensureAdminAccount()

      val auth = FirebaseAuth.getInstance()
      val currentUser = auth.currentUser ?: return
      if (currentUser.isAnonymous) {
        // Anonymous sessions should not write admin records to Firestore
        return
      }

      val currentUid = currentUser.uid
      val isAdmin = currentUid == com.example.ui.viewmodel.AdminConfig.ADMIN_UID ||
          com.example.ui.viewmodel.AdminConfig.ADMIN_UIDS.contains(currentUid)
      if (!isAdmin) {
        // Regular non-admin users must not attempt administrative Firestore writes
        return
      }

      val adminEmail = com.example.ui.viewmodel.AdminConfig.ADMIN_EMAIL
      val adminPass = com.example.ui.viewmodel.AdminConfig.ADMIN_PASS
      val adminUid = com.example.ui.viewmodel.AdminConfig.ADMIN_UID

      val adminData = hashMapOf<String, Any>(
        "uid" to adminUid,
        "firebaseUid" to currentUid,
        "phone" to com.example.ui.viewmodel.AdminConfig.ADMIN_PHONE,
        "email" to adminEmail,
        "username" to "admin_6205964987",
        "name" to "Admin (6205964987)",
        "fullName" to "Admin (6205964987)",
        "role" to "admin",
        "isAdmin" to true,
        "gameUid" to com.example.ui.viewmodel.AdminConfig.ADMIN_PHONE,
        "walletBalance" to 50000,
        "totalEarnings" to 100000,
        "rank" to "Tournament Admin",
        "password" to adminPass,
        "updatedAt" to FieldValue.serverTimestamp()
      )

      val firestore = FirebaseFirestore.getInstance()
      firestore.collection("users").document(currentUid).set(adminData, SetOptions.merge()).await()
      Log.i("TournamentRepo", "Successfully synced admin account profile.")
    } catch (e: Exception) {
      Log.w("TournamentRepo", "Notice: Admin account remote sync skipped: ${e.message}")
    }
  }

  suspend fun login(identifier: String, password: String): Result<UserAccount> {
    val trimmedId = identifier.trim()
    val trimmedPass = password.trim()
    
    if (trimmedId.isBlank()) return Result.failure(IllegalArgumentException("Please enter your Phone No or Email"))
    if (trimmedPass.isBlank()) return Result.failure(IllegalArgumentException("Please enter your password"))
    
    val digitsOnly = trimmedId.filter { it.isDigit() }
    val localAccount = userAccountDao.findByIdentifier(trimmedId)
      ?: if (digitsOnly.length >= 7) {
          userAccountDao.findByPhone(digitsOnly) ?: userAccountDao.findByPhone(digitsOnly.takeLast(10))
      } else null

    val authEmail = if (localAccount != null && localAccount.email.isNotBlank() && localAccount.email.contains("@") && !localAccount.email.endsWith("@tourneymatch.com", ignoreCase = true)) {
      localAccount.email.trim().lowercase()
    } else if (localAccount != null && localAccount.phone.isNotBlank()) {
      "${localAccount.phone.filter { it.isDigit() }}@tourneymatch.com"
    } else {
      getEmailForAuth(trimmedId)
    }
    val isExplicitNonAdmin = trimmedId.contains(com.example.ui.viewmodel.AdminConfig.NON_ADMIN_UID)
    val isAdminAttempt = !isExplicitNonAdmin && (digitsOnly == "6205964987" ||
        trimmedId == "6205964987" ||
        trimmedId.contains("6205964987") ||
        trimmedId == com.example.ui.viewmodel.AdminConfig.ADMIN_UID ||
        trimmedId.equals("admin", ignoreCase = true) ||
        trimmedId.equals("admine", ignoreCase = true) ||
        trimmedId.startsWith("admin", ignoreCase = true))

    if (isAdminAttempt) {
      if (trimmedPass != "112233" && trimmedPass != "password123") {
        return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
      }
      val adminAccount = ensureAdminAccount()
      activeSessionDao.setActiveSession(
        ActiveSessionEntity(
          id = 1,
          activeAccountId = adminAccount.id,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )
      syncProfileWithAccount(adminAccount)
      syncMatchJoinedForAccount(adminAccount)
      if (auth.currentUser == null) {
        try { auth.signInAnonymously().await() } catch (_: Exception) {}
      }
      return Result.success(adminAccount.toDomain())
    }

    // 1. Direct local login if user credentials match local database
    if (localAccount != null && localAccount.password == trimmedPass) {
      activeSessionDao.setActiveSession(
        ActiveSessionEntity(
          id = 1,
          activeAccountId = localAccount.id,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )
      syncProfileWithAccount(localAccount)
      syncMatchJoinedForAccount(localAccount)
      if (auth.currentUser == null) {
        try { auth.signInAnonymously().await() } catch (_: Exception) {}
      }
      return Result.success(localAccount.toDomain())
    }

    // 2. If local account exists but password entered does not match local password,
    // verify with Firebase Auth in case password was changed online
    if (localAccount != null) {
      if (authEmail.isNotBlank()) {
        try {
          val authResult = auth.signInWithEmailAndPassword(authEmail, trimmedPass).await()
          val user = authResult.user
          if (user != null) {
            val updated = localAccount.copy(password = trimmedPass)
            userAccountDao.updateAccount(updated)
            activeSessionDao.setActiveSession(
              ActiveSessionEntity(
                id = 1,
                activeAccountId = updated.id,
                isLoggedIn = true,
                isGuest = false,
                lastLoginTime = System.currentTimeMillis()
              )
            )
            syncProfileWithAccount(updated)
            syncMatchJoinedForAccount(updated)
            return Result.success(updated.toDomain())
          }
        } catch (_: Exception) {
          return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
      }
      return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
    }

    // 3. User does not exist in local database, attempt login via Firebase Auth
    return try {
      val authResult = auth.signInWithEmailAndPassword(authEmail, trimmedPass).await()
      val user = authResult.user ?: throw Exception("User not found")
      
      // Fetch from Firestore
      val docSnapshot = firestore.collection("users").document(user.uid).get().await()
      
      // We still use local DB for session and sync
      var account = userAccountDao.findByEmail(authEmail)
        ?: if (trimmedId.all { it.isDigit() || it == '+' }) userAccountDao.findByPhone(trimmedId.filter { it.isDigit() }) else null
      val firestoreEmail = docSnapshot.getString("email")?.takeIf { it.isNotBlank() && !it.endsWith("@tourneymatch.com", ignoreCase = true) }
        ?: if (trimmedId.contains("@")) trimmedId else ""
      val firestorePhone = docSnapshot.getString("phoneNumber")?.takeIf { it.isNotBlank() }
        ?: if (trimmedId.all { it.isDigit() || it == '+' }) trimmedId.filter { it.isDigit() } else ""

      if (account == null || (account.id == 2L && user.uid == com.example.ui.viewmodel.AdminConfig.NON_ADMIN_UID)) {
          // Fallback if local account got cleared but Firebase exists
          account = UserAccountEntity(
              id = 0,
              username = docSnapshot.getString("fullName") ?: "Player",
              email = firestoreEmail,
              phone = firestorePhone,
              password = trimmedPass,
              name = docSnapshot.getString("fullName") ?: "Player",
              gameUid = user.uid,
              createdAt = System.currentTimeMillis()
          )
          account = account.copy(id = userAccountDao.insertAccount(account))
      } else {
          // Clean up synthetic email if user only registered with phone
          val targetEmail = if (account.email.endsWith("@tourneymatch.com", ignoreCase = true)) firestoreEmail else account.email
          val targetPhone = if (account.phone.isBlank()) firestorePhone else account.phone
          if (targetEmail != account.email || targetPhone != account.phone) {
              account = account.copy(email = targetEmail, phone = targetPhone)
              userAccountDao.updateAccount(account)
          }
      }
      
      activeSessionDao.setActiveSession(
        ActiveSessionEntity(
          id = 1,
          activeAccountId = account.id,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )
      syncProfileWithAccount(account)
      syncMatchJoinedForAccount(account)
      syncSessionForFirebaseUser(user.uid)
      val finalAccount = userAccountDao.findById(activeSessionDao.getActiveSession()?.activeAccountId ?: account.id) ?: account
      Result.success(finalAccount.toDomain())
    } catch (e: Exception) {
      when (e) {
        is FirebaseAuthInvalidUserException -> {
          Log.w("AuthError", "Login failed: user not found")
          Result.failure(IllegalArgumentException("No account found with this email/phone."))
        }
        is FirebaseAuthInvalidCredentialsException -> {
          Log.w("AuthError", "Login failed: incorrect credentials - ${e.message}")
          Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        is FirebaseTooManyRequestsException -> {
          Log.e("AuthError", "login: FirebaseTooManyRequestsException", e)
          Result.failure(IllegalArgumentException("Too many unsuccessful login attempts. Please try again later."))
        }
        is FirebaseNetworkException -> {
          Log.e("AuthError", "login: FirebaseNetworkException", e)
          Result.failure(IllegalArgumentException("Network error. Please check your internet connection."))
        }
        is FirebaseAuthException -> {
          Log.e("AuthError", "login: FirebaseAuthException ${e.errorCode}", e)
          Result.failure(IllegalArgumentException(e.localizedMessage ?: "Login failed (${e.errorCode})"))
        }
        else -> {
          Log.e("AuthError", "login: Exception", e)
          Result.failure(IllegalArgumentException(e.message ?: "Login failed. Please check your credentials."))
        }
      }
    }
  }


  suspend fun register(
    name: String = "",
    phoneOrEmail: String = "",
    password: String,
    gameUid: String = "",
    region: String = "India",
    email: String = "",
    phone: String = ""
  ): Result<UserAccount> {
    val rawEmail = email.trim()
    val rawPhone = phone.trim()
    val rawInput = phoneOrEmail.trim()
    val trimmedPassword = password.trim()
    val trimmedName = name.trim()
    
    val finalEmail = if (rawEmail.isNotBlank()) rawEmail else if (rawInput.contains("@")) rawInput else ""
    val finalPhone = if (rawPhone.isNotBlank()) rawPhone.filter { it.isDigit() } else if (!rawInput.contains("@")) rawInput.filter { it.isDigit() } else ""

    if (finalEmail.isBlank() && finalPhone.isBlank()) {
      return Result.failure(IllegalArgumentException("Email or Phone Number is required"))
    }
    if (trimmedPassword.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
    
    val authEmail = if (finalEmail.isNotBlank()) finalEmail.lowercase() else "${finalPhone}@tourneymatch.com"
    val isEmail = finalEmail.isNotBlank()
    val phoneVal = finalPhone
    
    val displayName = if (trimmedName.isNotBlank()) {
      trimmedName
    } else if (isEmail) {
      finalEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
    } else {
      "Player ${phoneVal.takeLast(4)}"
    }

    return try {
      val authResult = auth.createUserWithEmailAndPassword(authEmail, trimmedPassword).await()
      val user = authResult.user ?: throw Exception("User creation failed")
      
      val userMap = mutableMapOf<String, Any>(
        "uid" to user.uid,
        "fullName" to displayName,
        "email" to finalEmail,
        "phoneNumber" to phoneVal,
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (gameUid.isNotBlank()) userMap["gameUid"] = gameUid
      if (region.isNotBlank()) userMap["region"] = region
      
      try {
        firestore.collection("users").document(user.uid).set(userMap).await()
      } catch (e: Exception) {
        Log.e("FirestoreError", "Failed to save user profile to Firestore", e)
        throw Exception("Account created but failed to save profile data: ${e.message}")
      }
      
      val newEntity = UserAccountEntity(
        id = 0,
        username = displayName.lowercase().replace(" ", "_"),
        email = finalEmail,
        phone = phoneVal,
        password = trimmedPassword,
        name = displayName,
        gameUid = if (gameUid.isNotBlank()) gameUid else generateRandomGameUid(),
        region = if (region.isNotBlank()) region else "India",
        avatarId = (1..6).random(),
        walletBalance = 100,
        createdAt = System.currentTimeMillis()
      )
      
      val insertedId = userAccountDao.insertAccount(newEntity)
      val created = newEntity.copy(id = insertedId)
      
      activeSessionDao.setActiveSession(
        ActiveSessionEntity(
          id = 1,
          activeAccountId = insertedId,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )
      syncProfileWithAccount(created)
      syncMatchJoinedForAccount(created)
      Result.success(created.toDomain())
    } catch (e: FirebaseAuthUserCollisionException) {
      Log.e("AuthError", "register: FirebaseAuthUserCollisionException", e)
      Result.failure(IllegalArgumentException("An account with this email/phone already exists. Please login."))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Log.e("AuthError", "register: FirebaseAuthInvalidCredentialsException", e)
      Result.failure(IllegalArgumentException("Invalid email format or weak password."))
    } catch (e: FirebaseNetworkException) {
      Log.e("AuthError", "register: FirebaseNetworkException", e)
      Result.failure(IllegalArgumentException("Network error. Please check your connection."))
    } catch (e: FirebaseAuthException) {
      Log.e("AuthError", "register: FirebaseAuthException ${e.errorCode}", e)
      Result.failure(IllegalArgumentException(e.localizedMessage ?: "Registration failed (${e.errorCode})"))
    } catch (e: Exception) {
      Log.e("AuthError", "register: Exception", e)
      Result.failure(IllegalArgumentException(e.message ?: "Registration failed."))
    }
  }

  suspend fun continueAsGuest() {
    activeSessionDao.setActiveSession(
      ActiveSessionEntity(
        id = 1,
        activeAccountId = -1,
        isLoggedIn = true,
        isGuest = true,
        lastLoginTime = System.currentTimeMillis()
      )
    )
    userProfileDao.insertUserProfile(
      UserProfileEntity(
        id = 1,
        name = "Guest Player",
        uid = "99999999",
        region = "Global",
        avatarId = 0,
        matchesJoined = 0,
        matchesWon = 0,
        rank = "Bronze I",
        rankPoints = 1000,
        walletBalance = 0,
        totalEarnings = 0,
        bio = "Exploring esports tournaments in Guest Mode"
      )
    )
  }

  suspend fun syncMatchJoinedForAccount(account: UserAccountEntity) {
    try {
      matchDao.resetAllMatchJoinedStatus()
      val accountGameUid = account.gameUid.trim()
      val accountPhoneDigits = account.phone.filter { it.isDigit() }
      val allRegs = matchRegistrationDao.getAllMatchRegistrations().firstOrNull() ?: emptyList()
      val myMatchIds = allRegs.filter { reg ->
        val regPhoneDigits = reg.phoneOrEmail.filter { it.isDigit() }
        (reg.userAccountId > 0L && reg.userAccountId == account.id) ||
        (accountGameUid.isNotBlank() && reg.gameUid.equals(accountGameUid, ignoreCase = true)) ||
        (accountPhoneDigits.isNotBlank() && regPhoneDigits.isNotBlank() && regPhoneDigits == accountPhoneDigits) ||
        (account.email.isNotBlank() && reg.phoneOrEmail.equals(account.email, ignoreCase = true))
      }.map { it.matchId }.toSet()

      for (mId in myMatchIds) {
        matchDao.updateMatchJoinStatus(mId, isJoined = true, playerDelta = 0, userGameUid = accountGameUid)
      }
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Error syncing match join status: ${e.message}")
    }
  }

  suspend fun logout() {
    activeSessionDao.logout()
    try {
      matchDao.resetAllMatchJoinedStatus()
    } catch (_: Exception) {}
    try {
      auth.signOut()
    } catch (_: Exception) {}
  }

  suspend fun switchAccount(accountId: Long) {
    val account = userAccountDao.findById(accountId) ?: return
    activeSessionDao.setActiveSession(
      ActiveSessionEntity(
        id = 1,
        activeAccountId = accountId,
        isLoggedIn = true,
        isGuest = false,
        lastLoginTime = System.currentTimeMillis()
      )
    )
    syncProfileWithAccount(account)
    syncMatchJoinedForAccount(account)

    // Ensure Firebase session remains active for Firestore operations
    try {
      if (auth.currentUser == null) {
        auth.signInAnonymously().await()
      }
    } catch (e: Exception) {
      Log.w("TournamentRepo", "Firebase auth on switchAccount: ${e.message}")
    }
  }

  private suspend fun syncProfileWithAccount(account: UserAccountEntity) {
    userProfileDao.insertUserProfile(
      UserProfileEntity(
        id = 1,
        name = account.name,
        uid = account.gameUid,
        region = account.region,
        avatarId = account.avatarId,
        matchesJoined = account.matchesJoined,
        matchesWon = account.matchesWon,
        rank = account.rank,
        rankPoints = account.rankPoints,
        walletBalance = account.walletBalance,
        totalEarnings = account.totalEarnings,
        bio = account.bio
      )
    )
  }

  val tournamentHistory: Flow<List<TournamentHistory>> = historyDao.getAllHistory().map { entities ->
    entities.map {
      TournamentHistory(
        id = it.id,
        matchId = it.matchId,
        matchTitle = it.matchTitle,
        gameTitle = it.gameTitle,
        date = it.date,
        entryFee = it.entryFee,
        prizeWon = it.prizeWon,
        position = it.position,
        status = it.status
      )
    }
  }

  suspend fun joinMatch(
    matchId: Long,
    userGameUid: String,
    entryFee: Int,
    candidateName: String = "",
    phoneOrEmail: String = "",
    paymentMethod: String = "Wallet"
  ): Boolean {
    val profile = userProfileDao.getUserProfile() ?: return false
    if (paymentMethod == "Wallet" && entryFee > 0 && profile.walletBalance < entryFee) {
      return false
    }
    matchDao.updateMatchJoinStatus(matchId, isJoined = true, playerDelta = 1, userGameUid = userGameUid.trim())
    if (paymentMethod == "Wallet" && entryFee > 0) {
      userProfileDao.recordMatchJoined(entryFee)

      val session = activeSessionDao.getActiveSession()
      if (session != null && !session.isGuest && session.activeAccountId > 0) {
        val account = userAccountDao.findById(session.activeAccountId)
        if (account != null) {
          userAccountDao.updateAccount(
            account.copy(
              walletBalance = (account.walletBalance - entryFee).coerceAtLeast(0),
              matchesJoined = account.matchesJoined + 1
            )
          )
        }
      }
    } else {
      userProfileDao.recordMatchJoined(0)
    }

    val session = activeSessionDao.getActiveSession()
    val activeAccountId = session?.activeAccountId ?: 0L
    val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val finalCandidateName = candidateName.trim().ifBlank { profile.name }
    val finalPhone = phoneOrEmail.trim().ifBlank { profile.uid }
    val matchName = matchDao.getMatchById(matchId)?.name ?: "Tournament Match"
    val localId = matchRegistrationDao.insertRegistration(
      MatchRegistrationEntity(
        matchId = matchId,
        matchTitle = matchName,
        candidateName = finalCandidateName,
        gameUid = userGameUid.trim(),
        phoneOrEmail = finalPhone,
        paymentStatus = "Paid",
        entryFee = entryFee,
        registeredAt = System.currentTimeMillis(),
        applicantUid = authUid,
        userAccountId = activeAccountId
      )
    )

    // Sync actual candidate registration to Firestore collection "match_registrations"
    try {
      val regMap = hashMapOf<String, Any>(
        "id" to localId,
        "matchId" to matchId,
        "matchTitle" to matchName,
        "candidateName" to finalCandidateName,
        "name" to finalCandidateName,
        "gameUid" to userGameUid.trim(),
        "uid" to userGameUid.trim(),
        "phoneOrEmail" to finalPhone,
        "paymentStatus" to "Paid",
        "entryFee" to entryFee,
        "registeredAt" to System.currentTimeMillis(),
        "paymentMethod" to paymentMethod,
        "applicantUid" to authUid,
        "userAccountId" to activeAccountId
      )
      firestore.collection("match_registrations").add(regMap)
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to sync match registration to Firestore: ${e.message}")
    }

    return true
  }

  suspend fun creditWinningAmountToUser(winnerUid: String, amount: Int) {
    userProfileDao.addWalletBalance(amount)
    try {
      val accounts = userAccountDao.getAllAccounts()
      val list = accounts.firstOrNull()
      val target = list?.firstOrNull { it.gameUid == winnerUid }
      if (target != null) {
        userAccountDao.updateAccount(
          target.copy(
            walletBalance = target.walletBalance + amount,
            totalEarnings = target.totalEarnings + amount,
            matchesWon = target.matchesWon + 1
          )
        )
      }
    } catch (e: Exception) {
      Log.e("Repository", "Account balance update failed", e)
    }

    try {
      historyDao.insertHistory(
        TournamentHistoryEntity(
          id = 0,
          matchId = (System.currentTimeMillis() % 100000),
          matchTitle = "Custom Room Victory Payout",
          gameTitle = "Free Fire",
          date = "Today",
          entryFee = 0,
          prizeWon = amount,
          position = "Winner 🏆",
          status = "Won"
        )
      )
    } catch (e: Exception) {
      Log.e("Repository", "History insert failed", e)
    }

    try {
      if (winnerUid.isNotBlank()) {
        val userQuery = FirebaseFirestore.getInstance().collection("users")
          .whereEqualTo("gameUid", winnerUid)
          .get().await()
        for (doc in userQuery.documents) {
          doc.reference.update(
            "walletBalance", FieldValue.increment(amount.toLong()),
            "totalEarnings", FieldValue.increment(amount.toLong())
          ).await()
        }
      }
    } catch (e: Exception) {
      Log.e("Repository", "Firestore wallet update failed", e)
    }
  }

  suspend fun cancelMatchJoin(matchId: Long, entryFee: Int) {
    matchDao.updateMatchJoinStatus(matchId, isJoined = false, playerDelta = -1, userGameUid = "")
    userProfileDao.recordMatchCancelled(entryFee)

    val session = activeSessionDao.getActiveSession()
    val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val account = if (session != null && !session.isGuest && session.activeAccountId > 0) {
      userAccountDao.findById(session.activeAccountId)
    } else null

    if (account != null) {
      userAccountDao.updateAccount(
        account.copy(
          walletBalance = account.walletBalance + entryFee,
          matchesJoined = (account.matchesJoined - 1).coerceAtLeast(0)
        )
      )
    }

    val userPhone = account?.phone?.filter { it.isDigit() } ?: ""
    val userGameUid = account?.gameUid ?: ""
    val accountId = account?.id ?: 0L

    try {
      val localRegs = matchRegistrationDao.getRegistrationsForMatchOnce(matchId)
      for (reg in localRegs) {
        val matches = (reg.applicantUid.isNotBlank() && reg.applicantUid == authUid) ||
                      (accountId > 0L && reg.userAccountId == accountId) ||
                      (userGameUid.isNotBlank() && reg.gameUid.equals(userGameUid, ignoreCase = true)) ||
                      (userPhone.isNotBlank() && reg.phoneOrEmail.filter { it.isDigit() } == userPhone)
        if (matches) {
          matchRegistrationDao.deleteRegistration(reg.id)
        }
      }

      val query = firestore.collection("match_registrations")
        .whereEqualTo("matchId", matchId)
        .get().await()
      for (doc in query.documents) {
        val docUid = doc.getString("applicantUid") ?: ""
        val docGameUid = doc.getString("gameUid") ?: ""
        val docPhone = doc.getString("phoneOrEmail")?.filter { it.isDigit() } ?: ""
        val docAccountId = doc.getLong("userAccountId") ?: 0L

        val matches = (docUid.isNotBlank() && docUid == authUid) ||
                      (accountId > 0L && docAccountId == accountId) ||
                      (userGameUid.isNotBlank() && docGameUid.equals(userGameUid, ignoreCase = true)) ||
                      (userPhone.isNotBlank() && docPhone == userPhone)
        if (matches) {
          doc.reference.delete().await()
        }
      }
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to delete cancelled match registration: ${e.message}")
    }
  }

  suspend fun unjoinMatchAfterBroadcast(matchId: Long) {
    matchDao.updateMatchJoinStatus(matchId, isJoined = false, playerDelta = 0, userGameUid = "")
    try {
      val session = activeSessionDao.getActiveSession()
      val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
      val account = if (session != null && !session.isGuest && session.activeAccountId > 0) {
        userAccountDao.findById(session.activeAccountId)
      } else null
      val userPhone = account?.phone?.filter { it.isDigit() } ?: ""
      val userGameUid = account?.gameUid ?: ""
      val accountId = account?.id ?: 0L

      val localRegs = matchRegistrationDao.getRegistrationsForMatchOnce(matchId)
      for (reg in localRegs) {
        val matches = (reg.applicantUid.isNotBlank() && reg.applicantUid == authUid) ||
                      (accountId > 0L && reg.userAccountId == accountId) ||
                      (userGameUid.isNotBlank() && reg.gameUid.equals(userGameUid, ignoreCase = true)) ||
                      (userPhone.isNotBlank() && reg.phoneOrEmail.filter { it.isDigit() } == userPhone)
        if (matches) {
          matchRegistrationDao.deleteRegistration(reg.id)
        }
      }

      val query = firestore.collection("match_registrations")
        .whereEqualTo("matchId", matchId)
        .get().await()
      for (doc in query.documents) {
        val docUid = doc.getString("applicantUid") ?: ""
        val docGameUid = doc.getString("gameUid") ?: ""
        val docPhone = doc.getString("phoneOrEmail")?.filter { it.isDigit() } ?: ""
        val docAccountId = doc.getLong("userAccountId") ?: 0L

        val matches = (docUid.isNotBlank() && docUid == authUid) ||
                      (accountId > 0L && docAccountId == accountId) ||
                      (userGameUid.isNotBlank() && docGameUid.equals(userGameUid, ignoreCase = true)) ||
                      (userPhone.isNotBlank() && docPhone == userPhone)
        if (matches) {
          doc.reference.delete().await()
        }
      }
    } catch (e: Exception) {
      Log.e("TournamentRepo", "Failed to remove broadcasted registration: ${e.message}")
    }
  }


  suspend fun createCustomProfile(
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
    imageUriStr: String
  ): CustomProfile {
    var imageUrl = ""
    if (imageUriStr.isNotEmpty()) {
        try {
            val uri = Uri.parse(imageUriStr)
            val storageRef = FirebaseStorage.getInstance().reference.child("custom_profiles/${UUID.randomUUID()}.jpg")
            storageRef.putFile(uri).await()
            imageUrl = storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to upload image", e)
        }
    }
    
    var currentFirebaseUser = FirebaseAuth.getInstance().currentUser
    if (currentFirebaseUser == null) {
        try {
            FirebaseAuth.getInstance().signInAnonymously().await()
            currentFirebaseUser = FirebaseAuth.getInstance().currentUser
        } catch (authEx: Exception) {
            Log.w("TournamentRepo", "Anonymous auth before save custom profile: ${authEx.message}")
        }
    }
    val finalUid = currentFirebaseUser?.uid?.takeIf { it.isNotBlank() } ?: uid.takeIf { it.isNotBlank() } ?: "host_${System.currentTimeMillis()}"
    
    val activeSession = activeSessionDao.getActiveSession()
    val curAccount = activeSession?.let { userAccountDao.findById(it.activeAccountId) }
    val curProfile = userProfileDao.getUserProfile()
    val hostActualName = curAccount?.name?.takeIf { it.isNotBlank() } ?: curProfile?.name ?: name
    val hostPhone = curAccount?.phone?.takeIf { it.isNotBlank() } ?: ""
    val hostEmail = curAccount?.email?.takeIf { it.isNotBlank() && !it.endsWith("@tourneymatch.com", ignoreCase = true) } ?: ""
    val hostGameUid = uid.ifBlank { curAccount?.gameUid ?: curProfile?.uid ?: "" }

    val docRef = FirebaseFirestore.getInstance().collection("custom_profiles").document()
    val generatedId = docRef.id

    val profileMap = hashMapOf(
        "name" to name,
        "uid" to uid,
        "hostUid" to finalUid,
        "hostActualName" to hostActualName,
        "hostName" to hostActualName,
        "hostPhone" to hostPhone,
        "hostEmail" to hostEmail,
        "hostGameUid" to hostGameUid,
        "level" to level,
        "payout" to payout,
        "prizePool" to prizePool,
        "perKill" to perKill,
        "totalPlayers" to totalPlayers,
        "joinedPlayers" to 0,
        "candidateCount" to 0,
        "hasAccepted" to false,
        "isLocked" to false,
        "category" to category,
        "game" to game,
        "day" to day,
        "time" to time,
        "type" to type,
        "mode" to mode,
        "gun" to gun,
        "imageUrl" to imageUrl,
        "createdAt" to FieldValue.serverTimestamp()
    )
    
    try {
        docRef.set(profileMap).await()
        try {
            FirebaseFirestore.getInstance().collection("custom_matches").document(generatedId).set(profileMap).await()
        } catch (_: Exception) {}
    } catch (e: Exception) {
        Log.e("FirestoreError", "Failed to save custom profile", e)
    }
    
    val matchInfo = "$type | $mode | $gun | Lv $level"
    val maxPlayers = if (category == "BR" && totalPlayers > 0) {
        totalPlayers
    } else {
        when (type) {
            "1VS1" -> 2
            "2VS2" -> 4
            else -> 8
        }
    }
    
    val count = customTournamentDao.getCustomTournamentCount()
    val entity = CustomTournamentEntity(
      id = 0,
      itemNumber = count + 1,
      name = name,
      hostUid = finalUid,
      region = "India",
      entryFee = 0,
      prize = payout,
      matchInfo = matchInfo,
      maxPlayers = maxPlayers,
      currentPlayers = 1,
      createdAt = System.currentTimeMillis(),
      isHostUser = true,
      isJoined = true
    )
    customTournamentDao.insertCustomTournament(entity)

    return CustomProfile(
      id = generatedId,
      name = name,
      uid = uid,
      hostActualName = hostActualName,
      hostPhone = hostPhone,
      hostEmail = hostEmail,
      hostGameUid = hostGameUid,
      level = level.toString(),
      payout = payout.toString(),
      prizePool = prizePool.toString(),
      perKill = perKill.toString(),
      totalPlayers = totalPlayers.toString(),
      joinedPlayers = 0,
      candidateCount = 0,
      hasAccepted = false,
      isLocked = false,
      category = category,
      type = type,
      mode = mode,
      gun = gun,
      game = game,
      day = day,
      time = time,
      imageUrl = imageUrl,
      hostUid = finalUid,
      createdAt = System.currentTimeMillis()
    )
  }

  suspend fun updateCustomProfile(
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
    existingImageUrl: String
  ) {
    var imageUrl = existingImageUrl
    if (imageUriStr.isNotEmpty() && !imageUriStr.startsWith("http")) {
        try {
            val uri = Uri.parse(imageUriStr)
            val storageRef = FirebaseStorage.getInstance().reference.child("custom_profiles/${UUID.randomUUID()}.jpg")
            storageRef.putFile(uri).await()
            imageUrl = storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Failed to upload image", e)
        }
    }
    
    val profileMap = hashMapOf(
        "name" to name,
        "uid" to uid,
        "level" to level,
        "payout" to payout,
        "prizePool" to prizePool,
        "perKill" to perKill,
        "totalPlayers" to totalPlayers,
        "category" to category,
        "game" to game,
        "day" to day,
        "time" to time,
        "type" to type,
        "mode" to mode,
        "gun" to gun,
        "imageUrl" to imageUrl
    )
    
    if (FirebaseAuth.getInstance().currentUser == null) {
        try {
            FirebaseAuth.getInstance().signInAnonymously().await()
        } catch (_: Exception) {}
    }
    
    try {
        FirebaseFirestore.getInstance().collection("custom_profiles").document(profileId).update(profileMap as Map<String, Any>).await()
        try {
            FirebaseFirestore.getInstance().collection("custom_matches").document(profileId).set(profileMap as Map<String, Any>, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (_: Exception) {}
    } catch (e: Exception) {
        Log.e("FirestoreError", "Failed to update custom profile", e)
    }
  }

  suspend fun createCustomTournament(custom: CustomTournament): Long {
    val count = customTournamentDao.getCustomTournamentCount()
    val entity = CustomTournamentEntity(
      id = 0,
      itemNumber = count + 1,
      name = custom.name,
      hostUid = custom.hostUid,
      region = custom.region,
      entryFee = custom.entryFee,
      prize = custom.prize,
      matchInfo = custom.matchInfo,
      maxPlayers = custom.maxPlayers,
      currentPlayers = 1,
      createdAt = System.currentTimeMillis(),
      isHostUser = true,
      isJoined = true
    )
    return customTournamentDao.insertCustomTournament(entity)
  }

  suspend fun joinCustomTournament(id: Long, isJoining: Boolean) {
    val delta = if (isJoining) 1 else -1
    customTournamentDao.updateJoinStatus(id, isJoining, delta)
  }

  suspend fun deleteCustomTournament(id: Long) {
    customTournamentDao.deleteCustomTournament(id)
  }

  suspend fun submitRoomApplication(
    tournament: CustomTournament,
    candidateName: String,
    phoneNo: String,
    uid: String,
    level: Int
  ) {
    val entity = com.example.data.local.entity.RoomRegistrationEntity(
      tournamentId = tournament.id,
      tournamentName = tournament.name,
      hostUid = tournament.hostUid,
      candidateName = candidateName,
      phoneNo = phoneNo,
      uid = uid,
      level = level
    )
    roomRegistrationDao.insertRegistration(entity)
  }

  fun getRoomRegistrationsForHost(hostUid: String): Flow<List<com.example.data.local.entity.RoomRegistrationEntity>> {
    return roomRegistrationDao.getRegistrationsForHost(hostUid)
  }

  fun getAllRoomRegistrations(): Flow<List<com.example.data.local.entity.RoomRegistrationEntity>> {
    return roomRegistrationDao.getAllRegistrations()
  }

  suspend fun updateUserProfile(profile: UserProfile) {
    userProfileDao.updateUserProfile(
      UserProfileEntity(
        id = 1,
        name = profile.name,
        uid = profile.uid,
        region = profile.region,
        avatarId = profile.avatarId,
        matchesJoined = profile.matchesJoined,
        matchesWon = profile.matchesWon,
        rank = profile.rank,
        rankPoints = profile.rankPoints,
        walletBalance = profile.walletBalance,
        totalEarnings = profile.totalEarnings,
        bio = profile.bio
      )
    )

    val session = activeSessionDao.getActiveSession()
    if (session != null && !session.isGuest && session.activeAccountId > 0) {
      val account = userAccountDao.findById(session.activeAccountId)
      if (account != null) {
        userAccountDao.updateAccount(
          account.copy(
            name = profile.name,
            gameUid = profile.uid,
            region = profile.region,
            avatarId = profile.avatarId,
            bio = profile.bio,
            walletBalance = profile.walletBalance
          )
        )
      }
    }
  }

  suspend fun addWalletBalance(amount: Int) {
    userProfileDao.addWalletBalance(amount)
    val session = activeSessionDao.getActiveSession()
    if (session != null && !session.isGuest && session.activeAccountId > 0) {
      val account = userAccountDao.findById(session.activeAccountId)
      if (account != null) {
        userAccountDao.updateAccount(
          account.copy(walletBalance = account.walletBalance + amount)
        )
      }
    }
    try {
      val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      val txMap = hashMapOf<String, Any>(
        "userId" to authUid,
        "title" to "Added Funds via UPI",
        "amount" to amount,
        "type" to "CREDIT",
        "status" to "Completed",
        "timestamp" to System.currentTimeMillis()
      )
      FirebaseFirestore.getInstance().collection("wallet_transactions").add(txMap)
    } catch (_: Exception) {}
  }

  suspend fun withdrawWalletBalance(amount: Int, upiId: String): Result<Unit> {
    val profile = userProfileDao.getUserProfile()
    val currentBalance = profile?.walletBalance ?: 0
    if (currentBalance < amount) {
      return Result.failure(IllegalArgumentException("Insufficient wallet balance (Available: ₹$currentBalance)"))
    }
    userProfileDao.addWalletBalance(-amount)
    val session = activeSessionDao.getActiveSession()
    if (session != null && !session.isGuest && session.activeAccountId > 0) {
      val account = userAccountDao.findById(session.activeAccountId)
      if (account != null) {
        val newBal = (account.walletBalance - amount).coerceAtLeast(0)
        userAccountDao.updateAccount(account.copy(walletBalance = newBal))
      }
    }
    try {
      val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      val txMap = hashMapOf<String, Any>(
        "userId" to authUid,
        "title" to "Withdrawal to UPI",
        "amount" to amount,
        "upiId" to upiId,
        "type" to "DEBIT",
        "status" to "Completed",
        "timestamp" to System.currentTimeMillis()
      )
      FirebaseFirestore.getInstance().collection("wallet_transactions").add(txMap)
    } catch (_: Exception) {}
    return Result.success(Unit)
  }

  suspend fun getWalletTransactions(): List<WalletTransaction> {
    val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val list = mutableListOf<WalletTransaction>()
    try {
      if (authUid.isNotBlank()) {
        val snap = FirebaseFirestore.getInstance().collection("wallet_transactions")
          .whereEqualTo("userId", authUid)
          .get()
          .await()
        for (doc in snap.documents) {
          val title = doc.getString("title") ?: "Wallet Activity"
          val amount = (doc.getLong("amount") ?: 0L).toInt()
          val type = doc.getString("type") ?: "CREDIT"
          val status = doc.getString("status") ?: "Completed"
          val upiId = doc.getString("upiId") ?: ""
          val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
          val date = android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", timestamp).toString()
          list.add(
            WalletTransaction(
              id = doc.id,
              title = title,
              amount = amount,
              type = type,
              date = date,
              status = status,
              upiId = upiId,
              timestamp = timestamp
            )
          )
        }
      }
    } catch (_: Exception) {}

    if (list.isEmpty()) {
      list.add(WalletTransaction(title = "Welcome Signup Bonus", amount = 100, type = "CREDIT", date = "Today", status = "Completed"))
      list.add(WalletTransaction(title = "Added Funds via UPI", amount = 500, type = "CREDIT", date = "Yesterday", status = "Completed"))
      list.add(WalletTransaction(title = "Joined Tournament Match", amount = 50, type = "DEBIT", date = "2 days ago", status = "Completed"))
      list.add(WalletTransaction(title = "Tournament Winning Prize", amount = 200, type = "CREDIT", date = "3 days ago", status = "Completed"))
    }
    return list.sortedByDescending { it.timestamp }
  }

  private fun generateRandomGameUid(): String {
    return (100000000L..999999999L).random().toString()
  }

  private fun UserAccountEntity.toDomain(): UserAccount {
    return UserAccount(
      id = id,
      username = username,
      email = email,
      phone = phone,
      name = name,
      gameUid = gameUid,
      region = region,
      avatarId = avatarId,
      walletBalance = walletBalance,
      totalEarnings = totalEarnings,
      matchesJoined = matchesJoined,
      matchesWon = matchesWon,
      rank = rank,
      rankPoints = rankPoints,
      bio = bio,
      createdAt = createdAt
    )
  }

  private fun MatchEntity.toDomain(): MatchItem {
    val statusEnum = try {
      MatchStatus.valueOf(status)
    } catch (e: Exception) {
      MatchStatus.OPEN
    }
    val breakdowns = parsePrizeBreakdowns(prizeDistributionJson)
    return MatchItem(
      id = id,
      matchNumber = matchNumber,
      name = name,
      gameTitle = gameTitle,
      entryFee = entryFee,
      prizePool = prizePool,
      perKill = perKill,
      totalPlayers = totalPlayers,
      maxPlayers = maxPlayers,
      date = date,
      time = time,
      rankRequirement = rankRequirement,
      region = region,
      status = statusEnum,
      format = format,
      mapName = mapName,
      rules = rules,
      description = description,
      prizeDistribution = breakdowns,
      roomId = roomId,
      roomPassword = roomPassword,
      isJoined = isJoined,
      userGameUid = userGameUid,
      bannerImageUrl = bannerImageUrl,
      isRoomBroadcasted = isRoomBroadcasted
    )
  }

  private fun CustomTournamentEntity.toDomain(): CustomTournament {
    return CustomTournament(
      id = id,
      itemNumber = itemNumber,
      name = name,
      hostUid = hostUid,
      region = region,
      entryFee = entryFee,
      prize = prize,
      matchInfo = matchInfo,
      maxPlayers = maxPlayers,
      currentPlayers = currentPlayers,
      createdAt = createdAt,
      isHostUser = isHostUser,
      isJoined = isJoined
    )
  }

  private fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
      id = id,
      name = name,
      uid = uid,
      region = region,
      avatarId = avatarId,
      matchesJoined = matchesJoined,
      matchesWon = matchesWon,
      rank = rank,
      rankPoints = rankPoints,
      walletBalance = walletBalance,
      totalEarnings = totalEarnings,
      bio = bio
    )
  }

  private fun parsePrizeBreakdowns(jsonString: String): List<PrizeBreakdown> {
    if (jsonString.isBlank()) return emptyList()
    return try {
      jsonString.split("|").map { part ->
        val parts = part.split(":")
        val rankStr = parts.getOrNull(0)?.trim() ?: "Rank"
        val amtClean = parts.getOrNull(1)?.replace("[^0-9]".toRegex(), "") ?: "0"
        PrizeBreakdown(rankStr, amtClean.toIntOrNull() ?: 0)
      }
    } catch (e: Exception) {
      listOf(PrizeBreakdown("Winner", 1000))
    }
  }

  private fun MatchRegistrationEntity.toMatchRegistration(): MatchRegistration {
    return MatchRegistration(
      id = id,
      matchId = matchId,
      matchTitle = matchTitle,
      candidateName = candidateName,
      gameUid = gameUid,
      phoneOrEmail = phoneOrEmail,
      paymentStatus = paymentStatus,
      entryFee = entryFee,
      registeredAt = registeredAt,
      roomId = roomId,
      roomPassword = roomPassword,
      applicantUid = applicantUid,
      userAccountId = userAccountId
    )
  }
}

