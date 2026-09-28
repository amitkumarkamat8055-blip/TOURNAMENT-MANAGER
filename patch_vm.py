import re

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "r") as f:
    content = f.read()

target = """              id = doc.id,
              name = data["name"] as? String ?: "",
              uid = data["uid"] as? String ?: "",
              level = data["level"] as? String ?: "",
              payout = data["payout"] as? String ?: "",
              type = data["type"] as? String ?: "",
              mode = data["mode"] as? String ?: "",
              gun = data["gun"] as? String ?: "",
              imageUrl = data["imageUrl"] as? String ?: "",
              hostUid = data["uid"] as? String ?: ""
            )"""

replacement = """              id = doc.id,
              name = data["name"] as? String ?: "",
              uid = data["uid"] as? String ?: "",
              level = data["level"]?.toString() ?: "",
              payout = data["payout"]?.toString() ?: "",
              type = data["type"] as? String ?: "",
              mode = data["mode"] as? String ?: "",
              gun = data["gun"] as? String ?: "",
              imageUrl = data["imageUrl"] as? String ?: "",
              hostUid = data["hostUid"] as? String ?: (data["uid"] as? String ?: "")
            )"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced in VM!")
else:
    print("Target not found in VM")

with open("app/src/main/java/com/example/ui/viewmodel/TournamentViewModel.kt", "w") as f:
    f.write(content)
