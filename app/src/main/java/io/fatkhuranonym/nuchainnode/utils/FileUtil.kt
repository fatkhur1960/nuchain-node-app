package io.fatkhuranonym.nuchainnode.utils

import android.content.Context
import io.fatkhuranonym.nuchainnode.data.Prefs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileUtil(private val context: Context, private val baseDir: String) {
    fun prepare() {
        val execFileName = Prefs.EXECUTABLE_FILE
        val libCppFileName = Prefs.LIB_CPP_FILE

        val filesDir = File(baseDir)
        val libDir = File(baseDir, "lib")

        val execFile = File(filesDir.path, execFileName)
        val libCppFile = File(libDir.path, libCppFileName)

        // create directory if not exists
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }

        // create lib directory
        if (!libDir.exists()) {
            libDir.mkdirs()
        }

        // copy executable file from assets
        if (!execFile.exists()) {
            val input = getFileFromAssets(context, execFileName)
            copyFile(input, execFile)
        }

        // copy libc++_shared.so
        if (!libCppFile.exists()) {
            val input = getFileFromAssets(context, libCppFileName)
            copyFile(input, libCppFile)
        }

        // set executable file permission
        execFile.setExecutable(true, false)
    }

    @Throws(IOException::class)
    private fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also { file ->
            if (!file.exists()) {
                file.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    private fun copyFile(input: File, output: File) {
        val src = FileInputStream(input).channel
        val dst = FileOutputStream(output).channel
        dst.transferFrom(src, 0, src.size())
        src.close()
        dst.close()
    }
}