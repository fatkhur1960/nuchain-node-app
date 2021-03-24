package io.fatkhuranonym.nuchainnode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import io.fatkhuranonym.nuchainnode.data.*
import org.greenrobot.eventbus.EventBus

class NodeServiceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG_BROADCAST_RECEIVER = "NodeServiceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prefs = NodePrefs(context)
            if (prefs.nodeStatus == NodeStatus.STOPPED && prefs.nodeNameExists) {
                val message = "Restarting node service"
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
            Log.e(TAG_BROADCAST_RECEIVER, ex.message, ex)
        }
    }
}