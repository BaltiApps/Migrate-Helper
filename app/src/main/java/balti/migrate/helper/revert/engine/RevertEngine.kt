package balti.migrate.helper.revert.engine

import android.content.Context
import android.os.AsyncTask
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.revert.OnRevert
import balti.migrate.helper.revert.RevertSettingsActivity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RevertEngine(private val settingsObject: SettingsPacketKotlin, context: Context) :
        AsyncTask<Any, Any, Any>() {

    private var suProcess : Process? = null
    private val allErrors by lazy { ArrayList<String>(0) }

    private var onRevert: OnRevert = context as OnRevert

    override fun doInBackground(vararg params: Any?): Any {

        suProcess = Runtime.getRuntime().exec("su")
        suProcess?.let {

            val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
            val errorStream = BufferedReader(InputStreamReader(it.errorStream))

            for (s in settingsObject.internalPackets) {
                if (RevertSettingsActivity.cancelRevert) break

                s.commandsToRestore.forEach { c ->
                    suInputStream.write("$c \n")
                }
                suInputStream.flush()
            }

            suInputStream.write("exit\n")
            suInputStream.flush()

            while (true) {
                val line = errorStream.readLine()
                if (line == null) break
                else allErrors.add(line)
            }
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        onRevert.onRevert(allErrors)
    }

}