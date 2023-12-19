package com.netplus.netpos_contactless_flutter_sample.utils

import android.annotation.SuppressLint
import android.os.Build
import com.danbamitale.epmslib.entities.KeyHolder
import com.google.gson.Gson
import com.netpluspay.nibssclient.models.UserData
import com.pixplicity.easyprefs.library.Prefs
import java.lang.reflect.Method

object AppUtils {
    const val KEY_HOLDER = "KEY_HOLDER"
    const val CONFIG_DATA = "CONFIG_DATA"
    const val ERROR_TAG = "ERROR_TAG===>"
    const val TAG_MAKE_PAYMENT = "TAG_MAKE_PAYMENT"
    const val TAG_CHECK_BALANCE = "TAG_CHECK_BALANCE"
    const val PAYMENT_SUCCESS_DATA_TAG = "PAYMENT_SUCCESS_DATA_TAG"
    const val PAYMENT_ERROR_DATA_TAG = "PAYMENT_ERROR_DATA_TAG"
    const val TAG_TERMINAL_CONFIGURATION = "TAG_TERMINAL_CONFIGURATION"
    const val CARD_HOLDER_NAME = "CUSTOMER"
    const val POS_ENTRY_MODE = "051"

    @SuppressLint("PrivateApi")
    fun getDeviceSerialNumber(): String {
        var serialNumber: String?
        try {
            val c = Class.forName("android.os.SystemProperties")
            val get: Method = c.getMethod("get", String::class.java)
            serialNumber = get.invoke(c, "gsm.sn1")?.toString()
            if (serialNumber == "") serialNumber = get.invoke(c, "ril.serialnumber")?.toString()
            if (serialNumber == "") serialNumber = get.invoke(c, "ro.serialno")?.toString()
            if (serialNumber == "") serialNumber = get.invoke(c, "sys.serialnumber")?.toString()
            if (serialNumber == "") serialNumber = Build.SERIAL

            // If none of the methods above worked
            if (serialNumber == "") serialNumber = null
        } catch (e: Exception) {
            e.printStackTrace()
            serialNumber = null
        }
        return serialNumber ?: "12345678901234"
    }

    fun getSampleUserData() = UserData(
        "Netplus", // => Just a string for your business
        "Netplus", // => Just a string for your business
        "5de231d9-1be0-4c31-8658-6e15892f2b83",
        "2033ALZP",
        "0123456789ABC",
        // getDeviceSerialNumber(),
        "Marwa Lagos", // => Just a string for your business Address
        "Test Account", // => Just a string for your business
        "",
        "",
        "",
    )

    fun getSavedKeyHolder(): KeyHolder? {
        val savedKeyHolderInStringFormat = Prefs.getString(KEY_HOLDER)
        return Gson().fromJson(savedKeyHolderInStringFormat, KeyHolder::class.java)
    }
}