package com.afraz.campusos.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.math.BigDecimal
import java.math.RoundingMode

object UpiDeepLinkHelper {
    fun buildPaymentUri(friendUpiId: String, friendName: String, note: String, amountPaise: Long): Uri {
        require(friendUpiId.isNotBlank()) { "UPI ID is required" }
        require(amountPaise > 0) { "Payment amount must be positive" }
        val rupees = BigDecimal.valueOf(amountPaise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN)
        return Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", friendUpiId.trim())
            .appendQueryParameter("pn", friendName.trim())
            .appendQueryParameter("tn", note.trim())
            .appendQueryParameter("am", rupees.toPlainString())
            .appendQueryParameter("cu", "INR")
            .build()
    }

    fun settle(context: Context, friendUpiId: String, friendName: String, note: String, amountPaise: Long): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, buildPaymentUri(friendUpiId, friendName, note, amountPaise))
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No UPI app is installed", Toast.LENGTH_LONG).show()
            false
        }
    }
}
