package io.fatkhuranonym.nuchainnode

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlin.system.exitProcess

object PopUpView {
    // PopupWindow display method
    @SuppressLint("InflateParams")
    fun showPopupWindow(view: View, callback: (nodeName: String) -> Unit) {
        // Create a View object yourself through inflater
        val inflater =
            view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_form, null)

        // Specify the length and width through constants
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.MATCH_PARENT

        // Create a window with our parameters
        val popupWindow = PopupWindow(popupView, width, height, true)

        // Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        popupWindow.isOutsideTouchable = false
        popupWindow.setOnDismissListener {
            return@setOnDismissListener
        }

        // Initialize the elements of our window, install the handler
        val nodeName = popupView.findViewById<EditText>(R.id.nodeName)
        val buttonEdit = popupView.findViewById<Button>(R.id.createNode)
        val buttonExit = popupView.findViewById<Button>(R.id.exitApp)
        buttonEdit.setOnClickListener {
            if (nodeName.text.isNullOrEmpty()) {
                Toast.makeText(view.context, "You did not enter a node name", Toast.LENGTH_LONG)
                    .show()
            } else {
                popupWindow.dismiss()
                callback(nodeName.text.toString())
            }
        }
        buttonExit.setOnClickListener {
            exitProcess(0)
        }
    }
}