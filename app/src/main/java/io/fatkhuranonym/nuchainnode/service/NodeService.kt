package io.fatkhuranonym.nuchainnode.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import io.fatkhuranonym.nuchainnode.MainActivity
import io.fatkhuranonym.nuchainnode.R
import io.fatkhuranonym.nuchainnode.data.*
import io.fatkhuranonym.nuchainnode.lib.ViewStateStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class NodeService : LifecycleService() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private val useCase = MainUseCase(Repository())
    private var currentNode: Node? = null
    private var store = ViewStateStore(Node())
    private lateinit var prefs: NodePrefs

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "NUCHAIN_NODE_SERVICE_CHANNEL"
        private const val NOTIFICATION_ID = 1960
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d("NodeService", "Some component want to bind with the service")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("NodeService", "onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action

            Log.d("NodeService", "using an intent with action $action")
            when (action) {
                ServiceAction.START.name -> startService()
                ServiceAction.STOP.name -> stopService()
                else -> Log.d(
                    "NodeService",
                    "This should never happen. No action in the received intent"
                )
            }
        } else {
            Log.d(
                "NodeService",
                "with a null intent. It has been probably restarted by the system."
            )
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        prefs = NodePrefs(this)
        EventBus.getDefault().register(this)

        listenNode()

        // Create notification
        val notification = notifBuilder().also {
            if (prefs.nodeNameExists) {
                it.setContentTitle(prefs.nodeName)
                val status = when (prefs.nodeStatus) {
                    NodeStatus.RUNNING -> "Running"
                    else -> "Stopped"
                }
                it.setContentText(status)
            }
        }
        startForeground(NOTIFICATION_ID, notification.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.setNodeStatus(NodeStatus.STOPPED)
        EventBus.getDefault().unregister(this)

        val intent = Intent()
        intent.action = "RestartNodeService"
        sendBroadcast(intent)

        Toast.makeText(this, "Node Service Stopped", Toast.LENGTH_SHORT).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNodeAction(event: Pair<NodeAction, Node>) {
        val notifBuilder = notifBuilder()
        val (action, node) = event

        node.also {
            notifBuilder.setContentTitle(it.name)

            val broadcastIntent = Intent(application, NotifBroadcastReceiver::class.java)
            val startIntent: PendingIntent =
                broadcastIntent.let { actionIntent ->
                    actionIntent.action = NodeAction.START.name
                    PendingIntent.getBroadcast(
                        this@NodeService,
                        0,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            val stopIntent: PendingIntent =
                broadcastIntent.let { actionIntent ->
                    actionIntent.action = NodeAction.STOP.name
                    PendingIntent.getBroadcast(
                        this@NodeService,
                        0,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            val restartIntent: PendingIntent =
                broadcastIntent.let { actionIntent ->
                    actionIntent.action = NodeAction.RESTART.name
                    PendingIntent.getBroadcast(
                        this@NodeService,
                        0,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

            when (action) {
                NodeAction.START -> {
                    NotificationManagerCompat.from(this).apply {
                        notifBuilder.setContentText("Running")
                        notifBuilder.addAction(
                            R.drawable.ic_play_gray,
                            "Restart Node",
                            restartIntent
                        )
                        notifBuilder.addAction(R.drawable.ic_stop_gray, "Stop Node", stopIntent)
                        notify(NOTIFICATION_ID, notifBuilder.build())
                    }
                    startNode(it)
                }
                NodeAction.STOP -> {
                    NotificationManagerCompat.from(this).apply {
                        notifBuilder.setContentText("Stopped")
                        notifBuilder.addAction(R.drawable.ic_play_gray, "Start Node", startIntent)
                        notify(NOTIFICATION_ID, notifBuilder.build())
                    }
                    stopNode(it)
                }
                else -> {
                    EventBus.getDefault().post(Pair(NodeAction.STOP, node))
                    GlobalScope.launch {
                        delay(400)
                        EventBus.getDefault().post(Pair(NodeAction.START, node))
                    }
                }
            }
        }
    }

    private fun startService() {
        if (isServiceStarted) return
        Toast.makeText(this, "Node Service Started", Toast.LENGTH_SHORT).show()
        isServiceStarted = true

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NodeService::lock").apply {
                    acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            }
    }

    private fun stopService() {
        Log.d("NodeService", "Stopping service")

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d("NodeService", "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }

    @SuppressLint("CheckResult")
    private fun startNode(node: Node) {
        prefs.setNodeStatus(NodeStatus.RUNNING)
        currentNode = node.copy(status = prefs.nodeStatus)

        store.dispatchActions(useCase.startNode(filesDir.path, cacheDir.path, node))

    }

    private fun stopNode(node: Node) {
        store.dispatchActions(useCase.stopNode(node))
        GlobalScope.launch {
            delay(400)
            store.cancelChildren()
            prefs.setNodeStatus(NodeStatus.IDLE)
            currentNode = node.copy(status = prefs.nodeStatus)
        }
    }

    private fun listenNode() {
        val bcIntent = Intent(application.packageName)
        store.observe(this) { node ->
            if(currentNode?.status != node.status) {
                currentNode = node
            }
            bcIntent.putExtra("output_log", node.output)
            sendBroadcast(bcIntent)
        }
    }

    @SuppressLint("ServiceCast")
    private fun notifBuilder(): NotificationCompat.Builder {
        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Nuchain Node Service notifications channel",
                NotificationManager.IMPORTANCE_MIN
            ).let {
                it.description = "Nuchain Node Service channel"
                it
            }
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notifBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationCompat.Builder(
                this,
                NOTIFICATION_CHANNEL_ID
            ) else NotificationCompat.Builder(this)

        return notifBuilder
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notif_white)
            .setPriority(NotificationCompat.PRIORITY_MIN)
    }
}