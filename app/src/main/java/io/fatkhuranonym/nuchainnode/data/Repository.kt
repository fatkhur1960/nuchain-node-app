package io.fatkhuranonym.nuchainnode.data

import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

object Prefs {
    const val EXECUTABLE_FILE = "nuchain_aarch64"
    const val LIB_CPP_FILE = "libc++_shared.so"
}

class Repository {
    private var runner: Process? = null

    companion object {
        fun getBinaryVersion(baseDir: String): String {
            val cmd = listOf(
                "$baseDir/${Prefs.EXECUTABLE_FILE}",
                "--version"
            )
            val pb = ProcessBuilder(cmd).redirectErrorStream(true)
            //val env = pb.environment()
            //env["LD_LIBRARY_PATH"] = "$baseDir/lib"
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                Log.d("output", line.toString())
                if (line!!.isNotEmpty()) {
                    builder.append(line)
                }
            }
            return builder.toString()
        }
    }

    suspend fun startNodeAsync(baseDir: String, cacheDir: String, node: Node): Deferred<Scanner> =
        coroutineScope {
            async {
                val cmd = listOf(
                    "$baseDir/${Prefs.EXECUTABLE_FILE}",
                    "--base-path=$cacheDir",
                    "--unsafe-pruning=500",
                    "--validator",
                    "--name=${node.name}"
                )

                val process = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                val env = process.environment()
                env["LD_LIBRARY_PATH"] = "$baseDir/lib"

                runner = process.start()

                return@async Scanner(runner?.inputStream)
            }
        }

    suspend fun stopNodeAsync(node: Node): Deferred<Node> = coroutineScope {
        async {
            runner?.destroy()
            return@async node.copy(status = NodeStatus.STOPPED)
        }
    }
}
