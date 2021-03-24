package io.fatkhuranonym.nuchainnode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.fatkhuranonym.nuchainnode.data.Node
import io.fatkhuranonym.nuchainnode.data.NodeAction
import io.fatkhuranonym.nuchainnode.data.NodePrefs
import io.fatkhuranonym.nuchainnode.data.NodeStatus
import org.greenrobot.eventbus.EventBus

class NotifBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = NodePrefs(context)
        val node = Node(status = NodeStatus.RUNNING, name = prefs.nodeName)
        intent.action?.let { action ->
            EventBus.getDefault().post(Pair(NodeAction.valueOf(action), node))
        }
    }
}