import re

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "r") as f:
    content = f.read()

target = """    val finalUid = currentFirebaseUser?.uid ?: uid
    
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
    )"""

replacement = """    val finalUid = currentFirebaseUser?.uid ?: ""
    
    val profileMap = hashMapOf(
        "name" to name,
        "uid" to uid,
        "hostUid" to finalUid,
        "level" to level,
        "payout" to payout,
        "regFee" to regFee,
        "type" to type,
        "mode" to mode,
        "gun" to gun,
        "imageUrl" to imageUrl,
        "createdAt" to FieldValue.serverTimestamp()
    )"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced in repo!")
else:
    print("Target not found in repo")

with open("app/src/main/java/com/example/data/repository/TournamentRepository.kt", "w") as f:
    f.write(content)
