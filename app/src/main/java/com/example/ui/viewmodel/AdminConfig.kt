package com.example.ui.viewmodel

object AdminConfig {
    // Admin UID and phone credentials
    const val ADMIN_UID = "RdLDfdXOeEazslnOpYWQLNpmKag1"
    val ADMIN_UIDS = setOf(
        "RdLDfdXOeEazslnOpYWQLNpmKag1",
        "6205964987"
    )

    // Explicit non-admin / regular candidate user that must be separated from admin account
    const val NON_ADMIN_UID = "pS59Zf07eOairnpJEmsCQRLmEfY2"
    val NON_ADMIN_UIDS = setOf(
        "pS59Zf07eOairnpJEmsCQRLmEfY2"
    )

    const val ADMIN_ID = "6205964987"
    const val ADMIN_PHONE = "6205964987"
    const val ADMIN_EMAIL = "6205964987@tourneymatch.com"
    const val ADMIN_PASS = "112233"
    var isAdminModeEnabled: Boolean = true

    fun isUserAdmin(
        firebaseUid: String?,
        accountPhone: String? = null,
        accountGameUid: String? = null,
        accountUsername: String? = null,
        accountId: Long = 0L
    ): Boolean {
        // Explicit non-admin user is always excluded from admin controls
        if (!firebaseUid.isNullOrBlank() && (firebaseUid == NON_ADMIN_UID || NON_ADMIN_UIDS.contains(firebaseUid))) {
            return false
        }
        // Account verification for Admin credentials (phone 6205964987 or username admin)
        val phoneDigits = accountPhone?.filter { it.isDigit() } ?: ""
        if (phoneDigits == ADMIN_PHONE ||
            accountPhone == ADMIN_PHONE ||
            accountGameUid == ADMIN_PHONE ||
            accountGameUid == ADMIN_UID ||
            accountUsername == "admin_6205964987" ||
            accountUsername?.equals("admin", ignoreCase = true) == true ||
            accountUsername?.startsWith("admin_", ignoreCase = true) == true) {
            return true
        }
        return false
    }
}

