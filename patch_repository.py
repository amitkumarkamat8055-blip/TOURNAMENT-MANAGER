import re

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "r") as f:
    content = f.read()

imports = """
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
"""
if "com.google.firebase.storage.FirebaseStorage" not in content:
    content = content.replace("import com.google.firebase.firestore.FirebaseFirestore", imports + "import com.google.firebase.firestore.FirebaseFirestore")

new_method = """
  suspend fun createCustomProfile(
    name: String,
    uid: String,
    level: Int,
    payout: Int,
    regFee: Int,
    type: String,
    mode: String,
    gun: String,
    imageUriStr: String
  ) {
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
    
    val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
    val finalUid = currentFirebaseUser?.uid ?: uid
    
    val profileMap = hashMapOf(
        "name" to name,
        "uid" to finalUid,
        "level" to level,
        "payout" to payout,
        "regFee" to regFee,
        "type" to type,
        "mode" to mode,
        "gun" to gun,
        "imageUrl" to imageUrl,
        "createdAt" to FieldValue.serverTimestamp()
    )
    
    try {
        FirebaseFirestore.getInstance().collection("custom_profiles").add(profileMap).await()
    } catch (e: Exception) {
        Log.e("FirestoreError", "Failed to save custom profile", e)
    }
    
    val matchInfo = "$type | $mode | $gun | Lv $level"
    val maxPlayers = when (type) {
        "1VS1" -> 2
        "2VS2" -> 4
        else -> 8
    }
    
    val count = customTournamentDao.getCustomTournamentCount()
    val entity = CustomTournamentEntity(
      id = 0,
      itemNumber = count + 1,
      name = name,
      hostUid = finalUid,
      region = "India",
      entryFee = regFee,
      prize = payout,
      matchInfo = matchInfo,
      maxPlayers = maxPlayers,
      currentPlayers = 1,
      createdAt = System.currentTimeMillis(),
      isHostUser = true,
      isJoined = true
    )
    customTournamentDao.insertCustomTournament(entity)
  }
"""

target = "  suspend fun createCustomTournament(custom: CustomTournament): Long {\n"
content = content.replace(target, new_method + "\n" + target)

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "w") as f:
    f.write(content)
