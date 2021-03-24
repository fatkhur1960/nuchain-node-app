package io.fatkhuranonym.nuchainnode.utils

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import io.fatkhuranonym.nuchainnode.R
import io.fatkhuranonym.nuchainnode.data.SessionResponse
import kotlinx.android.synthetic.main.progress_dialog.*
import org.json.JSONObject

class SessionUtil(private val context: Context) {
    fun getSessionKey() {
        val alertDialog = alertDialog()
        val body = JSONObject(
            mapOf(
                "id" to 1,
                "jsonrpc" to "2.0",
                "method" to "author_rotateKeys",
                "params" to listOf<String>()
            )
        ).toString()

        Fuel.post("http://localhost:9933")
            .header(Headers.CONTENT_TYPE, "application/json")
            .jsonBody(body)
            .also {
                Toast.makeText(context, "Generating session key", Toast.LENGTH_SHORT).show()
            }
            .responseObject(SessionResponse.Deserializer()) { _, _, result ->
                val (session, error) = result
                if (error != null) {
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                } else {
                    val resultKey = session?.result.toString()
                    alertDialog.show()
                    alertDialog.resultKey.setText(resultKey)
                    alertDialog.closeDialog.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    alertDialog.copyKey.setOnClickListener {
                        val clipboard =  context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("session_key", resultKey))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun alertDialog(): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customLayout: View = inflater.inflate(R.layout.progress_dialog, null)
        builder.setView(customLayout)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
}

