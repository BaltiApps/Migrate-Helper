package balti.migrate.helper.utilities

import android.content.Context

class WatcherInstallerCommands {

    companion object{
        fun getCommands(context: Context): ArrayList<String>{
            val commonTools = CommonToolsKotlin(context)
            val getPath = commonTools.unpackAssetToInternal("watcher.apk", "watcher.apk")
            return arrayListOf(
                    "verification_state=\"\$(settings get global package_verifier_enable)\"\n",
                    "if [[ -n \${verification_state} && \${verification_state} != \"null\" && \${verification_state} != \"0\" ]]; then\n",
                    "    settings put global package_verifier_enable 0\n",
                    "fi\n",
                    "mv $getPath /data/local/tmp/watcher.apk\n",
                    "pm install /data/local/tmp/watcher.apk 2>/dev/null\n",
                    "rm /data/local/tmp/watcher.apk\n",
                    "if [[ -n \${verification_state} && \${verification_state} != \"null\" && \${verification_state} != \"0\" ]]; then\n",
                    "    settings put global package_verifier_enable \${verification_state}\n",
                    "fi\n"
            )
        }
    }

}