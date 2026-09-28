import re

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "r") as f:
    content = f.read()

old_user_map = """      val userMap = hashMapOf(
        "uid" to user.uid,
        "fullName" to displayName,
        "email" to (if (isEmail) authEmail else ""),
        "phoneNumber" to phoneVal,
        "gameUid" to gameUid,
        "region" to region,
        "createdAt" to FieldValue.serverTimestamp()
      )"""

new_user_map = """      val userMap = mutableMapOf<String, Any>(
        "uid" to user.uid,
        "fullName" to displayName,
        "email" to (if (isEmail) authEmail else ""),
        "createdAt" to FieldValue.serverTimestamp()
      )
      if (phoneVal.isNotBlank()) userMap["phoneNumber"] = phoneVal
      if (gameUid.isNotBlank()) userMap["gameUid"] = gameUid
      if (region.isNotBlank()) userMap["region"] = region"""

content = content.replace(old_user_map, new_user_map)

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "w") as f:
    f.write(content)
