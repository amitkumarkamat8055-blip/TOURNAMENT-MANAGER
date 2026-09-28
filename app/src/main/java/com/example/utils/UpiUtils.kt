package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object UpiUtils {
    const val UPI_ID = "6205964987@fam"
    const val PAYEE_NAME = "Manto Esports"

    fun createUpiIntent(
        payeeAddress: String = UPI_ID,
        payeeName: String = PAYEE_NAME,
        transactionNote: String = "Match Entry Fee",
        amount: String,
        currency: String = "INR"
    ): Intent {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", payeeAddress)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("tn", transactionNote)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", currency)
            .build()
            
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        return intent
    }
}
