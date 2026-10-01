package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
  val status: String,
  val format: String,
  val mapName: String,
  val rules: String,
  val description: String,
  val prizeDistributionJson: String,
  val roomId: String,
  val roomPassword: String,
  val isJoined: Boolean = false,
  val userGameUid: String = "",
  val bannerImageUrl: String = "",
  val isRoomBroadcasted: Boolean = false
)

@Entity(tableName = "custom_tournaments")
data class CustomTournamentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val itemNumber: Int = 1,
  val name: String,
  val hostUid: String,
  val region: String,
  val entryFee: Int,
  val prize: Int,
  val matchInfo: String,
  val maxPlayers: Int,
  val currentPlayers: Int,
  val createdAt: Long,
  val isHostUser: Boolean,
  val isJoined: Boolean
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
  @PrimaryKey val id: Int = 1,
  val name: String,
  val uid: String,
  val region: String,
  val avatarId: Int,
  val matchesJoined: Int,
  val matchesWon: Int,
  val rank: String,
  val rankPoints: Int,
  val walletBalance: Int,
  val totalEarnings: Int,
  val bio: String
)

@Entity(tableName = "tournament_history")
data class TournamentHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val matchId: Long,
  val matchTitle: String,
  val gameTitle: String,
  val date: String,
  val entryFee: Int,
  val prizeWon: Int,
  val position: String,
  val status: String
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val username: String = "",
  val email: String = "",
  val phone: String = "",
  val password: String,
  val name: String = "Player",
  val gameUid: String = "",
  val region: String = "Asia",
  val avatarId: Int = 1,
  val walletBalance: Int = 0,
  val totalEarnings: Int = 0,
  val matchesJoined: Int = 0,
  val matchesWon: Int = 0,
  val rank: String = "Bronze I",
  val rankPoints: Int = 1000,
  val bio: String = "Esports tournament player",
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
  @PrimaryKey val id: Int = 1,
  val activeAccountId: Long,
  val isLoggedIn: Boolean = false,
  val isGuest: Boolean = false,
  val lastLoginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "room_registrations")
data class RoomRegistrationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val tournamentId: Long,
  val tournamentName: String,
  val hostUid: String,
  val candidateName: String,
  val phoneNo: String,
  val uid: String,
  val level: Int,
  val status: String = "Pending",
  val appliedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_registrations")
data class MatchRegistrationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val matchId: Long,
  val matchTitle: String,
  val candidateName: String,
  val gameUid: String,
  val phoneOrEmail: String,
  val paymentStatus: String = "Paid",
  val entryFee: Int = 0,
  val registeredAt: Long = System.currentTimeMillis(),
  val roomId: String = "",
  val roomPassword: String = "",
  val applicantUid: String = "",
  val userAccountId: Long = 0L
)
