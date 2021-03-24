package io.fatkhuranonym.nuchainnode.lib

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext

class Action<T>(private val f: T.() -> T) {
    operator fun invoke(t: T) = t.f()
}

class ViewStateStore<T : Any>(
    initialState: T
) : CoroutineScope {

    private val liveData = MutableLiveData<T>().apply {
        value = initialState
    }

    private val job = Job()

    override val coroutineContext: CoroutineContext = job + Dispatchers.IO

    fun observe(owner: LifecycleOwner, observer: (T) -> Unit) =
        liveData.observe(owner, { observer(it!!) })

    @MainThread
    fun dispatchState(state: T) {
        liveData.value = state
    }

    fun dispatchAction(f: suspend (T) -> Action<T>) {
        launch {
            val action = f(state())
            withContext(Dispatchers.Main) {
                dispatchState(action(state()))
            }
        }
    }

    fun dispatchActions(channel: ReceiveChannel<Action<T>>) {
        launch {
            channel.consumeEach { action ->
                withContext(Dispatchers.Main) {
                    dispatchState(action(state()))
                }
            }
        }
    }

    private fun state() = liveData.value!!

    fun cancel() = job.cancel()

    fun cancelChildren() = job.cancelChildren()
}