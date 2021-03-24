package io.fatkhuranonym.nuchainnode

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import io.fatkhuranonym.nuchainnode.data.Repository


object AboutDialog {
    // PopupWindow display method
    @SuppressLint("InflateParams", "SetTextI18n")
    fun show(context: Context, view: View) {
        val binVersionText = Repository.getBinaryVersion(context.filesDir.path)
        // Create a View object yourself through inflater
        val inflater =
            view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.about_dialog, null)

        // Specify the length and width through constants
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.MATCH_PARENT

        // Create a window with our parameters
        val popupWindow = PopupWindow(popupView, width, height)

        // Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        popupWindow.isOutsideTouchable = true

        // Initialize the elements of our window, install the handler
        val appVersion = popupView.findViewById<TextView>(R.id.appVersion)
        appVersion.text = "App Version: ${BuildConfig.VERSION_NAME}"
        val binVersion = popupView.findViewById<TextView>(R.id.binVersion)
        binVersion.text = "Binary Version: $binVersionText"
        val btnClose = popupView.findViewById<Button>(R.id.closeAboutDialog)
        val btnGh = popupView.findViewById<Button>(R.id.github)
        btnClose.setOnClickListener {
            popupWindow.dismiss()
        }
        btnGh.setOnClickListener {
            val url = context.resources.getString(R.string.github_url)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }
}