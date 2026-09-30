package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ActiveSessionDao
import com.example.data.local.dao.CustomTournamentDao
import com.example.data.local.dao.MatchDao
import com.example.data.local.dao.TournamentHistoryDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.ActiveSessionEntity
import com.example.data.local.entity.CustomTournamentEntity
import com.example.data.local.entity.MatchEntity
import com.example.data.local.entity.TournamentHistoryEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.RoomRegistrationEntity
import com.example.data.local.dao.RoomRegistrationDao
import com.example.data.local.entity.MatchRegistrationEntity
import com.example.data.local.dao.MatchRegistrationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    MatchEntity::class,
    CustomTournamentEntity::class,
    UserProfileEntity::class,
    TournamentHistoryEntity::class,
    UserAccountEntity::class,
    ActiveSessionEntity::class,
    RoomRegistrationEntity::class,
    MatchRegistrationEntity::class
  ],
  version = 8,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun matchDao(): MatchDao
  abstract fun customTournamentDao(): CustomTournamentDao
  abstract fun userProfileDao(): UserProfileDao
  abstract fun tournamentHistoryDao(): TournamentHistoryDao
  abstract fun userAccountDao(): UserAccountDao
  abstract fun activeSessionDao(): ActiveSessionDao
  abstract fun roomRegistrationDao(): RoomRegistrationDao
  abstract fun matchRegistrationDao(): MatchRegistrationDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN isRoomBroadcasted INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE match_registrations ADD COLUMN applicantUid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE match_registrations ADD COLUMN userAccountId INTEGER NOT NULL DEFAULT 0")
      }
    }

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "tournament_match_db"
        )
        .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
        .fallbackToDestructiveMigration()
        .addCallback(DatabaseCallback(scope))
        .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database)
          }
        }
      }
    }

    suspend fun populateInitialData(database: AppDatabase, includeMatches: Boolean = true) {
      val matchDao = database.matchDao()
      val customDao = database.customTournamentDao()
      val profileDao = database.userProfileDao()
      val historyDao = database.tournamentHistoryDao()
      val accountDao = database.userAccountDao()
      val sessionDao = database.activeSessionDao()

      // Initial demo account
      val demoAccountId = accountDao.insertAccount(
        UserAccountEntity(
          id = 1,
          username = "shadowstriker",
          email = "shadow@tournaments.com",
          phone = "9876543210",
          password = "password123",
          name = "ShadowStriker",
          gameUid = "548291047",
          region = "India / Asia",
          avatarId = 1,
          walletBalance = 850,
          totalEarnings = 4200,
          matchesJoined = 14,
          matchesWon = 5,
          rank = "Level 65",
          rankPoints = 3280,
          bio = "Competitive Battle Royale player | Leader of Team Phantom"
        )
      )

      // Pre-seeded Admin Account (UID / Phone: 6205964987)
      accountDao.insertAccount(
        UserAccountEntity(
          id = 2,
          username = "admin_6205964987",
          email = "6205964987@tourneymatch.com",
          phone = "6205964987",
          password = "112233",
          name = "Admin (6205964987)",
          gameUid = "6205964987",
          region = "India",
          avatarId = 0,
          walletBalance = 50000,
          totalEarnings = 100000,
          matchesJoined = 0,
          matchesWon = 0,
          rank = "Tournament Admin",
          rankPoints = 9999,
          bio = "Official Tournament Administrator & Payout Manager"
        )
      )

      // Initial active session (Default to Account 1 for candidate/player, or Account 2 ONLY for verified Admin)
      val currentAuthUid = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
      } catch (_: Exception) { "" }
      val initialAccountId = if (currentAuthUid == com.example.ui.viewmodel.AdminConfig.ADMIN_UID || com.example.ui.viewmodel.AdminConfig.ADMIN_UIDS.contains(currentAuthUid)) 2L else 1L
      sessionDao.setActiveSession(
        ActiveSessionEntity(
          id = 1,
          activeAccountId = initialAccountId,
          isLoggedIn = true,
          isGuest = false,
          lastLoginTime = System.currentTimeMillis()
        )
      )

      // Initial user profile
      profileDao.insertUserProfile(
        UserProfileEntity(
          id = 1,
          name = "ShadowStriker",
          uid = "548291047",
          region = "India / Asia",
          avatarId = 1,
          matchesJoined = 14,
          matchesWon = 5,
          rank = "Level 65",
          rankPoints = 3280,
          walletBalance = 850,
          totalEarnings = 4200,
          bio = "Competitive Battle Royale player | Leader of Team Phantom"
        )
      )

      // Initial Matches (Free Fire MAX exclusive)
      val sampleMatches = listOf(
        MatchEntity(
          id = 1,
          matchNumber = 1,
          name = "Daily Free Fire Bermuda Battle",
          gameTitle = "Free Fire MAX",
          entryFee = 50,
          prizePool = 2500,
          perKill = 25,
          totalPlayers = 78,
          maxPlayers = 100,
          date = "Today",
          time = "07:00 PM IST",
          rankRequirement = "Level 40+",
          region = "India (Asia)",
          status = "OPEN",
          format = "Squad (BR)",
          mapName = "Bermuda (Classic)",
          rules = "1. Free Fire MAX official esports settings.\n2. Room ID & Password shared 15 mins before match.\n3. Emulators strictly forbidden. Mobile players only.\n4. Screen recording or screenshot required for score verification.",
          description = "Premier Free Fire MAX Battle Royale championship with cash prizes for top squads and kill bonuses.",
          prizeDistributionJson = "1st: ₹1,200 | 2nd: ₹600 | 3rd: ₹400 | Per Kill: ₹25",
          roomId = "FF_8921",
          roomPassword = "PASS_BERMUDA",
          isJoined = false,
          userGameUid = "",
          isRoomBroadcasted = false
        ),
        MatchEntity(
          id = 2,
          matchNumber = 2,
          name = "Free Fire Clash Squad Arena",
          gameTitle = "Free Fire MAX",
          entryFee = 30,
          prizePool = 1500,
          perKill = 15,
          totalPlayers = 36,
          maxPlayers = 48,
          date = "Today",
          time = "08:30 PM IST",
          rankRequirement = "Level 50+",
          region = "India",
          status = "FAST_FILLING",
          format = "Clash Squad (4v4)",
          mapName = "Bermuda Remastered",
          rules = "1. Grenade spamming limit: 2 per round.\n2. Character skill active enabled.\n3. No roof climbing exploits.\n4. Disputes reviewed with screen recording.",
          description = "Fast-paced intense Clash Squad tournament. Winner takes the lions share and MVP rewards.",
          prizeDistributionJson = "1st: ₹900 | 2nd: ₹400 | Per Kill: ₹15",
          roomId = "CS_7720",
          roomPassword = "FF_CHAMP44",
          isJoined = false,
          userGameUid = "",
          isRoomBroadcasted = false
        ),
        MatchEntity(
          id = 3,
          matchNumber = 3,
          name = "Weekly Free Fire Lone Wolf Masters",
          gameTitle = "Free Fire MAX",
          entryFee = 40,
          prizePool = 2000,
          perKill = 0,
          totalPlayers = 92,
          maxPlayers = 100,
          date = "Tomorrow",
          time = "06:00 PM IST",
          rankRequirement = "Level 50+",
          region = "India",
          status = "FAST_FILLING",
          format = "Lone Wolf (1v1)",
          mapName = "Iron Cage",
          rules = "1. 1v1 pure gun skill.\n2. No gloo wall glitch exploits.\n3. Headshot accuracy & clutch plays.",
          description = "High octane 1v1 Free Fire Lone Wolf tournament with top fragger bonuses.",
          prizeDistributionJson = "1st: ₹1,100 | 2nd: ₹600 | 3rd: ₹300",
          roomId = "FF_LONE55",
          roomPassword = "WOLF_WINNER",
          isJoined = false,
          userGameUid = "",
          isRoomBroadcasted = false
        ),
        MatchEntity(
          id = 4,
          matchNumber = 4,
          name = "Sunday Free Fire Purgatory Solo",
          gameTitle = "Free Fire MAX",
          entryFee = 20,
          prizePool = 1000,
          perKill = 10,
          totalPlayers = 45,
          maxPlayers = 100,
          date = "Sunday",
          time = "05:00 PM IST",
          rankRequirement = "All Levels",
          region = "India",
          status = "UPCOMING",
          format = "Solo (BR)",
          mapName = "Purgatory",
          rules = "1. Solo lobby only. Teaming strictly prohibited.\n2. Fair play anti-cheat active.\n3. Results posted within 30 minutes.",
          description = "Prove your individual gun skill on Purgatory. Pure survival of the fittest.",
          prizeDistributionJson = "1st: ₹500 | 2nd: ₹250 | 3rd: ₹150 | Per Kill: ₹10",
          roomId = "FF_SOLO90",
          roomPassword = "SOLO_BOOYAH",
          isJoined = false,
          userGameUid = "",
          isRoomBroadcasted = false
        ),
        MatchEntity(
          id = 5,
          matchNumber = 5,
          name = "Weekly Free Fire Kalahari Pro League",
          gameTitle = "Free Fire MAX",
          entryFee = 100,
          prizePool = 5000,
          perKill = 0,
          totalPlayers = 16,
          maxPlayers = 48,
          date = "This Weekend",
          time = "09:00 PM IST",
          rankRequirement = "Level 60+",
          region = "India",
          status = "UPCOMING",
          format = "Squad (BR)",
          mapName = "Kalahari",
          rules = "1. Standard Free Fire competitive esports rules.\n2. Tactical pauses allowed.\n3. Booyah bonus awarded.",
          description = "Premier tactical mobile Free Fire showdown for serious semi-pro teams with a massive prize pool.",
          prizeDistributionJson = "1st: ₹3,000 | 2nd: ₹1,500 | MVP: ₹500",
          roomId = "FF_KAL990",
          roomPassword = "BOOYAH_PRO",
          isJoined = false,
          userGameUid = "",
          isRoomBroadcasted = false
        )
      )
      // Matches are strictly created and managed by the Admin in Firestore. No dummy sample matches are seeded.

      // Initial Custom Tournaments (Numbered 1, 2, 3... and 4 or more)
      val sampleCustoms = listOf(
        CustomTournamentEntity(
          id = 1,
          itemNumber = 1,
          name = "Clan Wars: Elite Scrims #1",
          hostUid = "982341029",
          region = "India / South Asia",
          entryFee = 50,
          prize = 1800,
          matchInfo = "Squad TPP | Erangel | 25 Teams Max",
          maxPlayers = 100,
          currentPlayers = 64,
          createdAt = System.currentTimeMillis() - 86400000,
          isHostUser = false,
          isJoined = false
        ),
        CustomTournamentEntity(
          id = 2,
          itemNumber = 2,
          name = "Hydra Custom Duo Cup #2",
          hostUid = "771920381",
          region = "India",
          entryFee = 25,
          prize = 800,
          matchInfo = "Duo TPP | Miramar | Sniper & AR only",
          maxPlayers = 50,
          currentPlayers = 38,
          createdAt = System.currentTimeMillis() - 72000000,
          isHostUser = false,
          isJoined = false
        ),
        CustomTournamentEntity(
          id = 3,
          itemNumber = 3,
          name = "Weekend Rush Solo Series #3",
          hostUid = "610294821",
          region = "Global / Asia",
          entryFee = 15,
          prize = 500,
          matchInfo = "Solo FPP | Livik Fast Pace | Zone Speed x1.5",
          maxPlayers = 52,
          currentPlayers = 41,
          createdAt = System.currentTimeMillis() - 36000000,
          isHostUser = false,
          isJoined = false
        ),
        CustomTournamentEntity(
          id = 4,
          itemNumber = 4,
          name = "Valorant Alpha Showdown #4",
          hostUid = "548291047",
          region = "India",
          entryFee = 100,
          prize = 3500,
          matchInfo = "5v5 Custom Lobby | Best of 3 Finals",
          maxPlayers = 40,
          currentPlayers = 25,
          createdAt = System.currentTimeMillis() - 18000000,
          isHostUser = true,
          isJoined = true
        ),
        CustomTournamentEntity(
          id = 5,
          itemNumber = 5,
          name = "Night Stalkers Arena #5",
          hostUid = "883920194",
          region = "SEA / India",
          entryFee = 30,
          prize = 1200,
          matchInfo = "Squad TPP | Sanhok Bootcamp Warfare",
          maxPlayers = 80,
          currentPlayers = 55,
          createdAt = System.currentTimeMillis() - 7200000,
          isHostUser = false,
          isJoined = false
        )
      )
      customDao.insertCustomTournaments(sampleCustoms)

      // Initial tournament history (Free Fire MAX)
      val sampleHistory = listOf(
        TournamentHistoryEntity(
          id = 1,
          matchId = 101,
          matchTitle = "Free Fire Bermuda Championship",
          gameTitle = "Free Fire MAX",
          date = "Yesterday",
          entryFee = 50,
          prizeWon = 1200,
          position = "1st Place 🏆",
          status = "Won"
        ),
        TournamentHistoryEntity(
          id = 2,
          matchId = 102,
          matchTitle = "Clash Squad Blitz Friday",
          gameTitle = "Free Fire MAX",
          date = "3 days ago",
          entryFee = 30,
          prizeWon = 400,
          position = "2nd Place 🥈",
          status = "Won"
        ),
        TournamentHistoryEntity(
          id = 3,
          matchId = 103,
          matchTitle = "Lone Wolf Masters Cup",
          gameTitle = "Free Fire MAX",
          date = "5 days ago",
          entryFee = 40,
          prizeWon = 0,
          position = "7th Place",
          status = "Completed"
        )
      )
      historyDao.insertHistories(sampleHistory)
    }
  }
}
