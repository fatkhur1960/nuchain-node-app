package io.fatkhuranonym.nuchainnode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import io.fatkhuranonym.nuchainnode.MainActivity
import io.fatkhuranonym.nuchainnode.data.*
import org.greenrobot.eventbus.EventBus


class BootDeviceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action) {
            startServiceDirectly(context)
        }
    }

    private fun startServiceDirectly(context: Context) {
        try {
            val prefs = NodePrefs(context)
            if (prefs.autoStart && prefs.nodeNameExists) {
                val message = "[BOOT] Starting node service"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                Intent(context, NodeService::class.java).also {
                    it.action = ServiceAction.START.name

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(it)
                    } else {
                        context.startService(it)
                    }

                    EventBus.getDefault().post(
                        Pair(
                            NodeAction.START,
                            Node(status = NodeStatus.RUNNING, name = prefs.nodeName)
                        )
                    )
                }
            }
        } catch (ex: InterruptedException) {
            Log.e(TAG_BOOT_BROADCAST_RECEIVER, ex.message, ex)
        }
    }

    companion object {
        private const val TAG_BOOT_BROADCAST_RECEIVER = "BOOT_BROADCAST_RECEIVER"
    }
}