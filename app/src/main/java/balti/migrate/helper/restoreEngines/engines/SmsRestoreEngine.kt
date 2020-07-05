package balti.migrate.helper.restoreEngines.engines

import android.provider.Telephony
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreEngines.utils.DBTools
import balti.migrate.helper.restoreSelectorActivity.containers.SmsPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_SMS_RESTORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_ADDRESS
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_BODY
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_CREATOR
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_DATE
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_DATE_SENT
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_ERROR
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_LOCKED
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_PERSON
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_PROTOCOL
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_READ
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_REPLY_PATH_PRESENT
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_SEEN
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_SERVICE_CENTER
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_STATUS
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_SUBJECT
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_TABLE_NAME
import balti.migrate.helper.utilities.constants.SmsDBConstant.Companion.SMS_TYPE
import balti.module.baltitoolbox.functions.Misc.getPercentage
import java.io.File

class SmsRestoreEngine(private val jobcode: Int,
                       private val smsPackets: ArrayList<SmsPacketKotlin>): ParentRestoreClass(EXTRA_PROGRESS_TYPE_SMS) {

    private val errors by lazy { ArrayList<String>(0) }
    private lateinit var currentPacket: SmsPacketKotlin
    private val dbTools by lazy { DBTools() }

    private var maxCount = 0

    private val tableName = SMS_TABLE_NAME
    private val uri = Telephony.Sms.CONTENT_URI

    private val projection = arrayOf(
            SMS_ADDRESS,
            SMS_BODY,
            SMS_TYPE,
            SMS_DATE,

            SMS_DATE_SENT,
            SMS_CREATOR,
            SMS_PERSON,
            SMS_PROTOCOL,

            SMS_SEEN,
            SMS_SERVICE_CENTER,
            SMS_STATUS,
            SMS_SUBJECT,

            SMS_ERROR,
            SMS_READ,
            SMS_LOCKED,
            SMS_REPLY_PATH_PRESENT
    )

    private val mirror = arrayOf(
            "${Telephony.Sms.ADDRESS}:s",
            "${Telephony.Sms.BODY}:s",
            "${Telephony.Sms.TYPE}:s",
            "${Telephony.Sms.DATE}:s",

            "${Telephony.Sms.DATE_SENT}:s",
            "${Telephony.Sms.CREATOR}:s",
            "${Telephony.Sms.PERSON}:s",
            "${Telephony.Sms.PROTOCOL}:s",

            "${Telephony.Sms.SEEN}:s",
            "${Telephony.Sms.SERVICE_CENTER}:s",
            "${Telephony.Sms.STATUS}:s",
            "${Telephony.Sms.SUBJECT}:s",

            "${Telephony.Sms.ERROR_CODE}:i",
            "${Telephony.Sms.READ}:i",
            "${Telephony.Sms.LOCKED}:i",
            "${Telephony.Sms.REPLY_PATH_PRESENT}:i"
    )

    private fun restoreSms(){

        publishProgress(true, "", "")

        val database = dbTools.getDataBase(currentPacket.smsDBFile)
        val cursor = dbTools.getTableRestoreCursor(database, tableName, projection) {
            maxCount = it
        }

        if (cursor == null)
            errors.add("$ERROR_SMS_RESTORE: null cursor for ${currentPacket.smsDBFile}")

        cursor?.let {
            errors.addAll(dbTools.restoreTable(engineContext.contentResolver,
                    it, uri, tableName, mirror, projection,
                    SMS_ADDRESS, ERROR_SMS_RESTORE, {return@restoreTable RestoreServiceKotlin.cancelAll},
                    { progress, taskLog ->
                        publishProgress(false, taskLog, getPercentage(progress, maxCount))
                    }
            ).apply {
                if (this.size == 0) File("${currentPacket.smsDBFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning
            })
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        for (packet in smsPackets) {
            if (RestoreServiceKotlin.cancelAll) break
            if (packet.isSelected) {
                currentPacket = packet
                restoreSms()
            }
        }
        return 0
    }

    override fun onProgressUpdate(vararg values: Any?) {
        super.onProgressUpdate(*values)
        val isReset = values[0] as Boolean
        if (isReset)
            resetBroadcast(false, engineContext.getString(R.string.restoring_sms))
        else broadcastProgress(currentPacket.smsDBFile.name, values[1].toString(), true, values[2] as Int)
    }

    override fun postExecuteFunction() {
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }

}