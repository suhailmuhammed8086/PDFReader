package com.example.pdfnotemate.utils
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.pdfnotemate.R
import com.google.android.material.snackbar.Snackbar

object Alerts {
    enum class AlertType {
        Success, Warning, Failure,
    }

    private const val ALERT_SUCCESS_COLOR = "#5CB85C"
    private const val ALERT_WARNING_COLOR = "#F2A91C"
    private const val ALERT_FAILURE_COLOR = "#FF6C6C"

    private fun showSnackBar(
        view: View,
        message: String,
        alertType: AlertType = AlertType.Success,
        duration: Int = Snackbar.LENGTH_SHORT,
    ) {
        try {
            val snackBar = Snackbar.make(view, message, duration)
            val context = view.context
            val snackBarView = snackBar.view
            val snackBarTextView = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            snackBarTextView?.typeface = ResourcesCompat.getFont(context, R.font.jakarta_sans_regular_400)
            snackBarTextView?.setTextColor(Color.WHITE)
            snackBarTextView?.textSize = 14f
            snackBarView.setBackgroundResource(R.drawable.custom_snackbar_background)
            when (alertType) {
                AlertType.Success ->
                    snackBarView.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ALERT_SUCCESS_COLOR))

                AlertType.Warning ->
                    snackBarView.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ALERT_WARNING_COLOR))

                AlertType.Failure ->
                    snackBarView.backgroundTintList = ColorStateList.valueOf(
                        Color.parseColor(
                            ALERT_FAILURE_COLOR,
                        ),
                    )
            }
            snackBar.show()
        } catch (_: Exception) {}
    }

    fun successSnackBar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ) {
        showSnackBar(view, message, AlertType.Success, duration)
    }
    fun warningSnackBar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ) {
        showSnackBar(view, message, AlertType.Warning, duration)
    }
    fun failureSnackBar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ) {
        showSnackBar(view, message, AlertType.Failure, duration)
    }
}
