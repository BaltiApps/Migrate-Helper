package balti.migrate.helper.restoreEngines.engines

import android.provider.CallLog
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreEngines.utils.DBTools
import balti.migrate.helper.restoreSelectorActivity.containers.CallsPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_CALLS_RESTORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_COUNTRY_ISO
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_DATA_USAGE
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_DATE
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_DURATION
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_FEATURES
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_GEOCODED_LOCATION
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_IS_READ
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_NEW
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_NUMBER
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_NUMBER_PRESENTATION
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_PHONE_ACCOUNT_COMPONENT_NAME
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_TABLE_NAME
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_TYPE
import balti.migrate.helper.utilities.constants.CallsDBConstants.Companion.CALLS_VOICEMAIL_URI
import java.io.File

class CallsRestoreEngine(private val jobcode: Int,
                         private val callsPackets: ArrayList<CallsPacketKotlin>): ParentRestoreClass(EXTRA_PROGRESS_TYPE_CALLS) {

    private val errors by lazy { ArrayList<String>(0) }
    private lateinit var currentPacket: CallsPacketKotlin
    private val dbTools by lazy { DBTools() }

    private var maxCount = 0

    private val tableName = CALLS_TABLE_NAME
    private val uri = CallLog.Calls.CONTENT_URI

    private val projection = arrayOf(
            CALLS_COUNTRY_ISO,
            CALLS_DATA_USAGE,
            CALLS_FEATURES,
            CALLS_GEOCODED_LOCATION,

            CALLS_IS_READ,
            CALLS_NUMBER,
            CALLS_NUMBER_PRESENTATION,

            CALLS_PHONE_ACCOUNT_COMPONENT_NAME,
            CALLS_TYPE,
            CALLS_VOICEMAIL_URI,

            CALLS_DATE,
            CALLS_DURATION,
            CALLS_NEW
    )

    private val mirror = arrayOf(
            "${CallLog.Calls.COUNTRY_ISO}:s",
            "${CallLog.Calls.DATA_USAGE}:s",
            "${CallLog.Calls.FEATURES}:s",
            "${CallLog.Calls.GEOCODED_LOCATION}:s",

            "${CallLog.Calls.IS_READ}:s",
            "${CallLog.Calls.NUMBER}:s",
            "${CallLog.Calls.NUMBER_PRESENTATION}:s",

            "${CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME}:s",
            "${CallLog.Calls.TYPE}:s",
            "${CallLog.Calls.VOICEMAIL_URI}:s",

            "${CallLog.Calls.DATE}:l",
            "${CallLog.Calls.DURATION}:l",
            "${CallLog.Calls.NEW}:i"
    )

    private fun restoreCalls(){

        publishProgress(true, "", "")

        val database = dbTools.getDataBase(currentPacket.callDBFile)
        val cursor = dbTools.getTableRestoreCursor(database, tableName, projection) {
            maxCount = it
        }

        if (cursor == null)
            errors.add("$ERROR_CALLS_RESTORE: null cursor for ${currentPacket.callDBFile}")

        cursor?.let {
            errors.addAll(dbTools.restoreTable(engineContext.contentResolver,
                    it, uri, tableName, mirror, projection,
                    CALLS_NUMBER, ERROR_CALLS_RESTORE, {return@restoreTable RestoreServiceKotlin.cancelAll},
                    { progress, taskLog ->
                        publishProgress(false, taskLog, commonTools.getPercentage(progress, maxCount))
                    }
            ).apply {
                if (this.size == 0) File("${currentPacket.callDBFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning
            })
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        for (packet in callsPackets){
            if (RestoreServiceKotlin.cancelAll) break
            if (packet.isSelected) {
                currentPacket = packet
                restoreCalls()
            }
        }
        return 0
    }

    override fun onProgressUpdate(vararg values: Any?) {
        super.onProgressUpdate(*values)
        val isReset = values[0] as Boolean
        if (isReset)
            resetBroadcast(false, engineContext.getString(R.string.restoring_calls))
        else broadcastProgress(currentPacket.callDBFile.name, values[1].toString(), true, values[2] as Int)
    }

    override fun postExecuteFunction() {
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }

}