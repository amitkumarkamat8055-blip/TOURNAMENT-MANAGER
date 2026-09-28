package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActiveSessionEntity
import com.example.data.local.entity.CustomTournamentEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.TournamentHistoryEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.RoomRegistrationEntity
import com.example.data.local.entity.MatchRegistrationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
  @Query("SELECT * FROM matches ORDER BY matchNumber ASC")
  fun getAllMatches(): Flow<List<MatchEntity>>

  @Query("SELECT * FROM matches ORDER BY matchNumber ASC")
  suspend fun getAllMatchesOnce(): List<MatchEntity>

  @Query("SELECT * FROM matches WHERE id = :id")
  suspend fun getMatchById(id: Long): MatchEntity?

  @Query("SELECT * FROM matches WHERE id = :id")
  fun getMatchByIdFlow(id: Long): Flow<MatchEntity?>

  @Query("SELECT * FROM matches WHERE isJoined = 1")
  fun getJoinedMatches(): Flow<List<MatchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatches(matches: List<MatchEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatch(match: MatchEntity): Long

  @Update
  suspend fun updateMatch(match: MatchEntity)

  @Query("UPDATE matches SET isJoined = :isJoined, totalPlayers = totalPlayers + :playerDelta, userGameUid = :userGameUid WHERE id = :id")
  suspend fun updateMatchJoinStatus(id: Long, isJoined: Boolean, playerDelta: Int, userGameUid: String)

  @Query("DELETE FROM matches WHERE id = :id")
  suspend fun deleteMatch(id: Long)

  @Query("SELECT COUNT(*) FROM matches")
  suspend fun getMatchCount(): Int

  @Query("UPDATE matches SET gameTitle = 'Free Fire MAX', name = 'Free Fire Bermuda Championship', mapName = 'Bermuda (Classic)', format = 'Squad (BR)' WHERE gameTitle LIKE '%BGMI%' OR gameTitle LIKE '%PUBG%' OR name LIKE '%Apex%'")
  suspend fun sanitizeBgmiMatches()

  @Query("UPDATE matches SET gameTitle = 'Free Fire MAX', name = 'Free Fire Lone Wolf 1v1 Masters', mapName = 'Iron Cage', format = 'Lone Wolf (1v1)' WHERE gameTitle LIKE '%COD%'")
  suspend fun sanitizeCodMatches()

  @Query("UPDATE matches SET gameTitle = 'Free Fire MAX', name = 'Free Fire Kalahari Pro League', mapName = 'Kalahari', format = 'Squad (BR)' WHERE gameTitle LIKE '%Valorant%'")
  suspend fun sanitizeValorantMatches()

  @Query("UPDATE matches SET isJoined = 0, userGameUid = ''")
  suspend fun resetAllMatchJoinedStatus()
}

@Dao
interface CustomTournamentDao {
  @Query("SELECT * FROM custom_tournaments ORDER BY itemNumber ASC, id ASC")
  fun getAllCustomTournaments(): Flow<List<CustomTournamentEntity>>

  @Query("SELECT * FROM custom_tournaments WHERE id = :id")
  suspend fun getCustomTournamentById(id: Long): CustomTournamentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomTournaments(tournaments: List<CustomTournamentEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomTournament(tournament: CustomTournamentEntity): Long

  @Query("DELETE FROM custom_tournaments WHERE id = :id")
  suspend fun deleteCustomTournament(id: Long)

  @Query("UPDATE custom_tournaments SET isJoined = :isJoined, currentPlayers = currentPlayers + :delta WHERE id = :id")
  suspend fun updateJoinStatus(id: Long, isJoined: Boolean, delta: Int)

  @Query("SELECT COUNT(*) FROM custom_tournaments")
  suspend fun getCustomTournamentCount(): Int
}

@Dao
interface UserProfileDao {
  @Query("SELECT * FROM user_profile WHERE id = 1")
  fun getUserProfileFlow(): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profile WHERE id = 1")
  suspend fun getUserProfile(): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUserProfile(userProfile: UserProfileEntity)

  @Update
  suspend fun updateUserProfile(userProfile: UserProfileEntity)

  @Query("UPDATE user_profile SET matchesJoined = matchesJoined + 1, walletBalance = walletBalance - :entryFee WHERE id = 1")
  suspend fun recordMatchJoined(entryFee: Int)

  @Query("UPDATE user_profile SET matchesJoined = CASE WHEN matchesJoined > 0 THEN matchesJoined - 1 ELSE 0 END, walletBalance = walletBalance + :refund WHERE id = 1")
  suspend fun recordMatchCancelled(refund: Int)

  @Query("UPDATE user_profile SET walletBalance = walletBalance + :amount WHERE id = 1")
  suspend fun addWalletBalance(amount: Int)
}

@Dao
interface TournamentHistoryDao {
  @Query("SELECT * FROM tournament_history ORDER BY id DESC")
  fun getAllHistory(): Flow<List<TournamentHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(history: TournamentHistoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistories(histories: List<TournamentHistoryEntity>)

  @Query("SELECT COUNT(*) FROM tournament_history")
  suspend fun getHistoryCount(): Int

  @Query("UPDATE tournament_history SET gameTitle = 'Free Fire MAX', matchTitle = 'Free Fire Bermuda Championship' WHERE gameTitle LIKE '%BGMI%' OR gameTitle LIKE '%PUBG%'")
  suspend fun sanitizeHistoryBgmi()

  @Query("UPDATE tournament_history SET gameTitle = 'Free Fire MAX', matchTitle = 'Free Fire Clash Squad Arena' WHERE gameTitle LIKE '%COD%'")
  suspend fun sanitizeHistoryCod()
}

@Dao
interface UserAccountDao {
  @Query("SELECT * FROM user_accounts WHERE LOWER(username) = LOWER(:identifier) OR LOWER(email) = LOWER(:identifier) OR phone = :identifier LIMIT 1")
  suspend fun findByIdentifier(identifier: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun findByEmail(email: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE phone = :phone LIMIT 1")
  suspend fun findByPhone(phone: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE gameUid = :gameUid LIMIT 1")
  suspend fun findByGameUid(gameUid: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE LOWER(username) = LOWER(:username) LIMIT 1")
  suspend fun findByUsername(username: String): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE id = :id")
  suspend fun findById(id: Long): UserAccountEntity?

  @Query("SELECT * FROM user_accounts WHERE id = :id")
  fun findByIdFlow(id: Long): Flow<UserAccountEntity?>

  @Query("SELECT * FROM user_accounts ORDER BY id ASC")
  fun getAllAccounts(): Flow<List<UserAccountEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccount(account: UserAccountEntity): Long

  @Update
  suspend fun updateAccount(account: UserAccountEntity)

  @Query("SELECT COUNT(*) FROM user_accounts")
  suspend fun getAccountCount(): Int
}

@Dao
interface ActiveSessionDao {
  @Query("SELECT * FROM active_session WHERE id = 1")
  fun getActiveSessionFlow(): Flow<ActiveSessionEntity?>

  @Query("SELECT * FROM active_session WHERE id = 1")
  suspend fun getActiveSession(): ActiveSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setActiveSession(session: ActiveSessionEntity)

  @Query("UPDATE active_session SET isLoggedIn = 0 WHERE id = 1")
  suspend fun logout()

  @Query("DELETE FROM active_session")
  suspend fun clearSession()
}

@Dao
interface RoomRegistrationDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRegistration(registration: RoomRegistrationEntity)

  @Query("SELECT * FROM room_registrations WHERE hostUid = :hostUid ORDER BY id DESC")
  fun getRegistrationsForHost(hostUid: String): Flow<List<RoomRegistrationEntity>>

  @Query("SELECT * FROM room_registrations ORDER BY id DESC")
  fun getAllRegistrations(): Flow<List<RoomRegistrationEntity>>
}

@Dao
interface MatchRegistrationDao {
  @Query("SELECT * FROM match_registrations WHERE matchId = :matchId ORDER BY id DESC")
  fun getRegistrationsForMatch(matchId: Long): Flow<List<MatchRegistrationEntity>>

  @Query("SELECT * FROM match_registrations WHERE matchId = :matchId")
  suspend fun getRegistrationsForMatchOnce(matchId: Long): List<MatchRegistrationEntity>

  @Query("SELECT * FROM match_registrations ORDER BY id DESC")
  fun getAllMatchRegistrations(): Flow<List<MatchRegistrationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRegistration(registration: MatchRegistrationEntity): Long

  @Query("UPDATE match_registrations SET paymentStatus = :status WHERE id = :id")
  suspend fun updatePaymentStatus(id: Long, status: String)

  @Query("UPDATE match_registrations SET roomId = :roomId, roomPassword = :password WHERE matchId = :matchId")
  suspend fun broadcastRoomCredentials(matchId: Long, roomId: String, password: String)

  @Query("DELETE FROM match_registrations WHERE id = :id")
  suspend fun deleteRegistration(id: Long)

  @Query("DELETE FROM match_registrations WHERE matchId = :matchId")
  suspend fun deleteRegistrationsForMatch(matchId: Long)

  @Query("DELETE FROM match_registrations WHERE candidateName IN ('ThunderStrike', 'ShadowViper', 'PhoenixFire', 'AlphaWolf', 'GhostRider')")
  suspend fun purgeDummyRegistrations()
}


