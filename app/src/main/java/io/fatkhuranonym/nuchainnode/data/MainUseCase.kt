package io.fatkhuranonym.nuchainnode.data

import android.util.Log
import io.fatkhuranonym.nuchainnode.lib.Action
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay


class MainUseCase(private val repository: Repository) {

    fun startNode(baseDir: String, cacheDir: String, node: Node): ReceiveChannel<Action<Node>> =
        produceActions {
            send {
                copy(
                    status = NodeStatus.RUNNING,
                    name = node.name,
                    output = "Running node ${node.name} as validator"
                )
            }

            try {
                val sc = repository.startNodeAsync(baseDir, cacheDir, node).await()
                while (sc.hasNextLine()) {
                    val output = sc.nextLine()
                    send { copy(output = output) }
                }
            } catch (e: Exception) {
                send { copy(output = e.localizedMessage ?: "IOException") }
            }
        }

    fun stopNode(node: Node): ReceiveChannel<Action<Node>> = produceActions {
        val newNode = repository.stopNodeAsync(node).await()
        send { copy(status = newNode.status, output = "Stopping node ${newNode.name}") }
        delay(300)
        send { copy(output = "Node ${newNode.name} stopped") }
    }
}

fun <T> produceActions(f: suspend ProducerScope<Action<T>>.() -> Unit): ReceiveChannel<Action<T>> =
    GlobalScope.produce(block = f)

suspend fun <T> ProducerScope<Action<T>>.send(f: T.() -> T) = send(Action(f))