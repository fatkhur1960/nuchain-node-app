package io.fatkhuranonym.nuchainnode.data

import android.content.Context
import com.orhanobut.hawk.Hawk

class NodePrefs(context: Context) {

    init {
        Hawk.init(context).build()
    }

    val nodeNameExists: Boolean = Hawk.contains(KEY_NODE_NAME)
    fun setNodeName(name: String) = Hawk.put(KEY_NODE_NAME, name)
    val nodeName: String get() = Hawk.get(KEY_NODE_NAME)

    fun setServiceStatus(status: ServiceAction) = Hawk.put(KEY_SERVICE_STATUS, status.name)
    val serviceStatus: ServiceAction get() {
        val status = Hawk.get(KEY_SERVICE_STATUS, ServiceAction.STOP.name)
        return ServiceAction.valueOf(status)
    }

    fun setNodeStatus(status: NodeStatus) = Hawk.put(KEY_NODE_STATUS, status.name)
    val nodeStatus: NodeStatus get() {
        val status = Hawk.get(KEY_NODE_STATUS, NodeStatus.IDLE.name)
        return NodeStatus.valueOf(status)
    }

    fun setAutoStart(value: Boolean) = Hawk.put(KEY_AUTO_START, value)
    val autoStart: Boolean get() = Hawk.get(KEY_AUTO_START, false)

    companion object {
        private const val KEY_NODE_NAME = "node_name"
        private const val KEY_NODE_STATUS = "node_status"
        private const val KEY_SERVICE_STATUS = "service_status"
        private const val KEY_AUTO_START = "auto_start"
    }
}