package io.fatkhuranonym.nuchainnode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.fatkhuranonym.nuchainnode.data.*
import io.fatkhuranonym.nuchainnode.service.NodeService
import io.fatkhuranonym.nuchainnode.utils.FileUtil
import io.fatkhuranonym.nuchainnode.utils.SessionUtil
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var adapter: ArrayAdapter<String>
    private var menuHandler: Menu? = null
    private var currentNode: Node = Node()
    private var outputs = mutableListOf<String>()
    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var prefs: NodePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        prefs = NodePrefs(this)
        FileUtil(this, filesDir.path).prepare()

        initAdapter()

        if (!prefs.nodeNameExists) {
            val rootView: View = findViewById(R.id.mainLayout)
            rootView.post {
                showPopup(rootView)
            }
        } else {
            currentNode = currentNode.copy(
                name = prefs.nodeName,
                status = prefs.nodeStatus
            )
            updateSubtitle()
        }

        actionOnService(ServiceAction.START)
        registerLocalReceiver()
    }

    private fun showPopup(rootView: View) {
        PopUpView.showPopupWindow(rootView) { nodeName ->
            prefs.setNodeName(nodeName)
            currentNode = currentNode.copy(name = nodeName)
            updateSubtitle()
        }
    }

    private fun initAdapter() {
        adapter = ArrayAdapter(this, R.layout.log_item, outputs)
        adapter.setNotifyOnChange(true)
        listView.adapter = adapter
    }

    private fun updateSubtitle() {
        supportActionBar?.apply {
            val status = when (currentNode.status) {
                NodeStatus.RUNNING -> "Running"
                else -> "Stopped"
            }
            subtitle = "${currentNode.name} ($status)"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        menuHandler = menu
        menuHandler?.findItem(R.id.autoStart)?.isChecked = prefs.autoStart
        menuHandler?.findItem(R.id.nodeAction)?.let {
            if (currentNode.status != NodeStatus.RUNNING) {
                it.setIcon(R.drawable.ic_play)
            } else {
                it.setIcon(R.drawable.ic_stop)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nodeAction -> {
                if (currentNode.status != NodeStatus.RUNNING) {
                    if (!prefs.nodeNameExists) {
                        val rootView: View = findViewById(R.id.mainLayout)
                        rootView.post {
                            showPopup(rootView)
                        }
                        Toast.makeText(this, "Please set node name first!", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        EventBus.getDefault()
                            .post(Pair(NodeAction.START, currentNode))
                    }
                } else {
                    EventBus.getDefault()
                        .post(Pair(NodeAction.STOP, currentNode))
                }
                true
            }
            R.id.fetchHexCode -> {
                if (currentNode.status != NodeStatus.RUNNING) {
                    Toast.makeText(this, "Node Service not started :3", Toast.LENGTH_LONG).show()
                } else {
                    SessionUtil(this).getSessionKey()
                }
                true
            }
            R.id.clearLogs -> {
                outputs.clear()
                adapter.notifyDataSetChanged()
                true
            }
            R.id.autoStart -> {
                val autoStart: Boolean = prefs.autoStart
                if (!autoStart) {
                    prefs.setAutoStart(true)
                    item.isChecked = true
                } else {
                    prefs.setAutoStart(false)
                    item.isChecked = false
                }

                true
            }
            R.id.about -> {
                val rootView: View = findViewById(R.id.mainLayout)
                AboutDialog.show(this, rootView)
                true
            }
            R.id.exitApp -> {
                if (prefs.nodeStatus == NodeStatus.RUNNING) {
                    EventBus.getDefault()
                        .post(Pair(NodeAction.STOP, currentNode))
                    actionOnService(ServiceAction.STOP)
                }
                finish()
                exitProcess(0)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun actionOnService(action: ServiceAction) {
        prefs.setServiceStatus(action)
        val serviceIntent = Intent(this, NodeService::class.java).let {
            it.action = action.name
            it.putExtra("node", currentNode)
            it
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
            return
        }
        startService(serviceIntent)
    }

    private fun registerLocalReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                onReceiveResult(intent.getStringExtra("output_log"))
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter(application.packageName))
    }

    fun onReceiveResult(outputLog: String?) {
        if (outputLog != null) {
            val limit = 70
            val size = outputs.size

            if (size > limit) {
                val newOutputs = arrayListOf<String>()
                newOutputs.addAll(outputs.subList(size - limit, size))
                outputs.clear()
                adapter.notifyDataSetChanged()
                outputs.addAll(newOutputs)
                adapter.notifyDataSetChanged()
            }
            outputs.add(outputLog)
            adapter.notifyDataSetChanged()

            if (outputLog.contains("panicked|error".toRegex(setOf(RegexOption.IGNORE_CASE)))) {
                EventBus.getDefault()
                    .post(Pair(NodeAction.STOP, currentNode))
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNodeAction(event: Pair<NodeAction, Node>) {
        val (action, _) = event
        when (action) {
            NodeAction.START -> {
                outputs.clear()
                adapter.notifyDataSetChanged()
                menuHandler?.findItem(R.id.nodeAction)?.setIcon(R.drawable.ic_stop)
                currentNode = currentNode.copy(status = NodeStatus.RUNNING)
            }
            NodeAction.STOP -> {
                menuHandler?.findItem(R.id.nodeAction)?.setIcon(R.drawable.ic_play)
                currentNode = currentNode.copy(status = NodeStatus.IDLE)
            }
            else -> {}
        }

        updateSubtitle()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver)
            } catch (e: Exception) {
                Log.e("Error", e.localizedMessage ?: "Failed unregistering broadcast receiver")
            }
        }
    }

}