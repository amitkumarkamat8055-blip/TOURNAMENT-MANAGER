#!/bin/bash
cat << 'PATCH_EOF' > /tmp/repo_patch.kt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ...
PATCH_EOF
