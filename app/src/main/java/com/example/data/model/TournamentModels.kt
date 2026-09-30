package com.example.data.model

enum class MatchStatus(val label: String) {
  UPCOMING("Upcoming"),
  OPEN("Registration Open"),
  FAST_FILLING("Fast Filling"),
  LIVE("Live Now"),
  COMPLETED("Completed")
}

data class PrizeBreakdown(
  val rank: String,
  val amount: Int
)

data class MatchItem(
  val id: Long = 0,
  val matchNumber: Int,
  val name: String,
  val gameTitle: String,
  val entryFee: Int,
  val prizePool: Int,
  val perKill: Int = 0,
  val totalPlayers: Int,
  val maxPlayers: Int,
  val date: String,
  val time: String,
  val rankRequirement: String,
  val region: String,
  val status: MatchStatus,
  val format: String, // e.g. "Squad (TPP)", "Solo", "Duo"
  val mapName: String,
  val rules: String,
  val description: String,
  val prizeDistribution: List<PrizeBreakdown>,
  val roomId: String = "TBA",
  val roomPassword: String = "TBA",
  val isJoined: Boolean = false,
  val userGameUid: String = "",
  val bannerImageUrl: String = "",
  val isRoomBroadcasted: Boolean = false
)

val MatchItem.isDailyMatch: Boolean
  get() = name.contains("Daily", ignoreCase = true) || 
          (!name.contains("Weekly", ignoreCase = true) && !description.contains("Weekly", ignoreCase = true))

fun MatchItem.getEffectivePerKill(): Int {
  if (perKill > 0) return perKill
  val found = prizeDistribution.find { it.rank.contains("Per Kill", ignoreCase = true) }?.amount
  if (found != null && found > 0) return found
  return when {
    entryFee >= 100 -> 50
    entryFee >= 50 -> 25
    entryFee >= 40 -> 20
    entryFee >= 30 -> 15
    entryFee >= 20 -> 10
    entryFee > 0 -> (entryFee / 2).coerceAtLeast(5)
    else -> 5
  }
}

data class CustomTournament(
  val id: Long = 0,
  val itemNumber: Int = 1,
  val name: String,
  val hostUid: String,
  val region: String,
  val entryFee: Int,
  val prize: Int,
  val matchInfo: String,
  val maxPlayers: Int = 100,
  val currentPlayers: Int = 1,
  val createdAt: Long = System.currentTimeMillis(),
  val isHostUser: Boolean = false,
  val isJoined: Boolean = false
)

data class UserProfile(
  val id: Int = 1,
  val name: String,
  val uid: String,
  val region: String,
  val avatarId: Int = 0,
  val matchesJoined: Int = 0,
  val matchesWon: Int = 0,
  val rank: String = "Level 50",
  val rankPoints: Int = 2450,
  val walletBalance: Int = 500,
  val totalEarnings: Int = 1250,
  val bio: String = "Esports enthusiast & competitive player"
)

data class TournamentHistory(
  val id: Long = 0,
  val matchId: Long,
  val matchTitle: String,
  val gameTitle: String,
  val date: String,
  val entryFee: Int,
  val prizeWon: Int,
  val position: String,
  val status: String
)

data class UserAccount(
  val id: Long = 0,
  val username: String = "",
  val email: String = "",
  val phone: String = "",
  val name: String = "Player",
  val gameUid: String = "",
  val region: String = "Asia",
  val avatarId: Int = 1,
  val walletBalance: Int = 0,
  val totalEarnings: Int = 0,
  val matchesJoined: Int = 0,
  val matchesWon: Int = 0,
  val rank: String = "Level 15",
  val rankPoints: Int = 1000,
  val bio: String = "Esports tournament player",
  val createdAt: Long = System.currentTimeMillis()
)

sealed class AuthStatus {
  object Initializing : AuthStatus()
  object LoggedOut : AuthStatus()
  data class LoggedIn(val account: UserAccount, val isGuest: Boolean = false) : AuthStatus()
}



data class CustomProfile(
  val id: String = "",
  val name: String = "",
  val uid: String = "",
  val hostActualName: String = "",
  val hostPhone: String = "",
  val hostEmail: String = "",
  val hostGameUid: String = "",
  val level: String = "",
  val payout: String = "",
  val prizePool: String = "",
  val perKill: String = "",
  val totalPlayers: String = "",
  val joinedPlayers: Int = 0,
  val candidateCount: Int = 0,
  val hasAccepted: Boolean = false,
  val isLocked: Boolean = false,
  val category: String = "Custom", // "Custom" or "BR"
  val type: String = "",
  val mode: String = "",
  val gun: String = "",
  val game: String = "",
  val day: String = "",
  val time: String = "",
  val imageUrl: String = "",
  val hostUid: String = "",
  val createdAt: Long = 0L
)

data class CustomProfileApplication(
  val id: String = "",
  val profileId: String = "",
  val profileName: String = "",
  val hostUid: String = "",
  val candidateName: String = "",
  val phone: String = "",
  val email: String = "",
  val uid: String = "",
  val level: String = "",
  val rank: String = "",
  val status: String = "Pending",
  val appliedAt: Long = 0L,
  val roomId: String? = null,
  val roomPassword: String? = null,
  val hostPaid: Boolean = false,
  val payout: String = "",
  val applicantUid: String = "",
  val resultScreenshot: String = "",
  val winnerName: String = "",
  val winnerUid: String = "",
  val isResultSubmitted: Boolean = false,
  val resultSubmittedBy: String = "",
  val reportReason: String = "",
  val reportDescription: String = "",
  val reportMediaUrl: String = "",
  val reportSubmittedBy: String = "",
  val isReported: Boolean = false,
  val winningAmountSent: Boolean = false,
  val winningAmountSentTo: String = "",
  val hostActualName: String = "",
  val hostPhone: String = "",
  val hostEmail: String = "",
  val hostGameUid: String = ""
)

data class MatchRegistration(
  val id: Long = 0,
  val matchId: Long = 0L,
  val matchTitle: String = "",
  val candidateName: String = "",
  val gameUid: String = "",
  val phoneOrEmail: String = "",
  val paymentStatus: String = "Paid",
  val entryFee: Int = 0,
  val registeredAt: Long = System.currentTimeMillis(),
  val roomId: String = "",
  val roomPassword: String = "",
  val applicantUid: String = "",
  val userAccountId: Long = 0L
)

data class WalletTransaction(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val amount: Int,
  val type: String, // "CREDIT" or "DEBIT"
  val date: String = "Today",
  val status: String = "Completed",
  val upiId: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

