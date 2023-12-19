package com.netplus.netpos_contactless_flutter_sample

import io.flutter.plugin.common.MethodChannel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.clearPinKey
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.google.gson.Gson
import com.netplus.netpos_contactless_flutter_sample.model.CardResult
import com.netplus.netpos_contactless_flutter_sample.model.Status
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.CARD_HOLDER_NAME
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.CONFIG_DATA
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.ERROR_TAG
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.KEY_HOLDER
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.PAYMENT_ERROR_DATA_TAG
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.PAYMENT_SUCCESS_DATA_TAG
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.POS_ENTRY_MODE
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.getSampleUserData
import com.netplus.netpos_contactless_flutter_sample.utils.AppUtils.getSavedKeyHolder
import com.netpluspay.contactless.sdk.start.ContactlessSdk
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult
import com.netpluspay.nibssclient.models.IsoAccountType
import com.netpluspay.nibssclient.models.MakePaymentParams
import com.netpluspay.nibssclient.models.UserData
import com.netpluspay.nibssclient.service.NetposPaymentClient
import com.pixplicity.easyprefs.library.Prefs
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivity : FlutterFragmentActivity() {
    private var amount: Double = 0.00
    private val CHANNEL = "netplus/contactless_sdk"
    private val gson: Gson = Gson()
    private var userData: UserData = getSampleUserData()
    private var cardData: CardData? = null
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var netposPaymentClient: NetposPaymentClient = NetposPaymentClient
    private lateinit var makePaymentResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var checkBalanceResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var flutterResult: MethodChannel.Result

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        // Setting up MethodChannel native counterpart to handle communication between Flutter and native Android code.
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            flutterResult = result
            handleMethodCall(call)
        }
    }

    private fun handleMethodCall(call: MethodCall) {
        when (call.method) {
            "startPayment" -> startPaymentForFlutter(call)
            "checkBalance" -> launchContactless(checkBalanceResultLauncher, 200.0) // Example amount
            else -> flutterResult.notImplemented()
        }
    }

    private fun startPaymentForFlutter(call: MethodCall) {
        amount = call.argument<Double>("amount") ?: return
        launchContactless(makePaymentResultLauncher, amount)
    }

    private fun checkBalance(cardResult: CardResult) {
        sendMessageToFlutter("showLoader", mapOf())
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE).also { cardD ->
                cardD.pinBlock = it.pinBlock
            }
        }
        compositeDisposable.add(
            netposPaymentClient.balanceEnquiry(this, cardData, IsoAccountType.SAVINGS.name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        sendMessageToFlutter("dismissLoader", mapOf())
                        val responseString = if (it.responseCode == Status.APPROVED.statusCode) {
                            "Response: APPROVED\nResponse Code: ${it.responseCode}\n\nAccount Balance:\n" + it.accountBalances.joinToString(
                                "\n",
                            ) { accountBalance ->
                                "${accountBalance.accountType}: ${
                                    accountBalance.amount.div(100).formatCurrencyAmount()
                                }"
                            }
                        } else {
                            "Response: ${it.responseMessage}\nResponse Code: ${it.responseCode}"
                        }
                        sendMessageToFlutter("updateStatus", mapOf("message" to responseString))

                    }
                    error?.let {
                        sendMessageToFlutter("dismissLoader", mapOf())
                        sendMessageToFlutter("showError", mapOf("message" to it.localizedMessage))
                    }
                },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).post {
            configureTerminal()
        }
        initializeResultLaunchers()
    }

    private fun handlePaymentResult(success: Boolean, message: String) {
        if (success) {
            sendResultToFlutter(flutterResult, true, "Payment Successful")
        } else {
            sendResultToFlutter(flutterResult, false, message)
        }
    }

    private fun initializeResultLaunchers() {
        makePaymentResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handlePaymentActivityResult(result)
            }

        checkBalanceResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleBalanceActivityResult(result)
            }
    }

    private fun handlePaymentActivityResult(result: ActivityResult) {
        val data: Intent? = result.data
        if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
            data?.let { i ->
                val amountToPay = amount // Retrieve this value correctly
                val cardReadData = i.getStringExtra("data")!!
                val cardResult = gson.fromJson(cardReadData, CardResult::class.java)

                Log.d("card_data_log_payment", cardReadData)
                Log.d("card_data_result_payment", gson.toJson(cardResult))
                Log.d("card_data_payment", "Payment: GOT HERE")

                // Call startPayment to initiate the payment process
                makePayment(amountToPay, cardResult)
            }
        } else if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
            data?.let { i ->
                val error = i.getStringExtra("data") ?: "Unknown Error"
                Timber.d("ERROR_TAG===>%s", error)

                // Call handlePaymentResult with failure status
                handlePaymentResult(false, error)
            }
        }
    }

    private fun handleBalanceActivityResult(result: ActivityResult) {
        // Handle balance activity result
        val data: Intent? = result.data
        if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
            data?.let { i ->
                val cardReadData = i.getStringExtra("data")!!
                val cardResult = gson.fromJson(cardReadData, CardResult::class.java)

                Log.d("card_data_log", cardReadData)
                Log.d("card_data_result", gson.toJson(cardResult))
                Log.d("card_data", "BALANCE: GOT HERE")
                checkBalance(cardResult)
            }
        }
        if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
            data?.let { i ->
                val error = i.getStringExtra("data")
                error?.let {
                    Timber.d("ERROR_TAG===>%s", it)
                    sendMessageToFlutter("showError", mapOf("message" to it))
                }
            }
        }
    }

    private fun makePayment(amount: Double, cardResult: CardResult) {
        sendMessageToFlutter("showLoader", mapOf())
        val amountToPay = (amount * 100).toLong()

        // Extract and prepare card data from cardResult
        val cardData = cardResult.cardReadResult.let {
            CardData(it.track2Data, it.iccString, it.pan, POS_ENTRY_MODE)
        }
        cardData.pinBlock = cardResult.cardReadResult.pinBlock

        // Prepare payment parameters, similar to your native makePayment
        val makePaymentParams = MakePaymentParams(
            amount = amountToPay,
            terminalId = userData.terminalId,
            cardData = cardData,
            accountType = IsoAccountType.SAVINGS
        )

        compositeDisposable.add(
            netposPaymentClient.makePayment(
                this,
                userData.terminalId,
                gson.toJson(makePaymentParams),
                cardResult.cardScheme,
                CARD_HOLDER_NAME,
                "TESTING_TESTING",
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { transactionWithRemark ->
                        sendMessageToFlutter("dismissLoader", mapOf())
                        sendMessageToFlutter(
                            "updatePaymentResult",
                            mapOf("message" to gson.toJson(transactionWithRemark))
                        )
                        Timber.d(
                            "$PAYMENT_SUCCESS_DATA_TAG%s",
                            gson.toJson(transactionWithRemark),
                        )
                    },
                    { throwable ->
                        sendMessageToFlutter("dismissLoader", mapOf())
                        sendMessageToFlutter(
                            "showError",
                            mapOf("message" to throwable.localizedMessage)
                        )
                        Timber.d(
                            "$PAYMENT_ERROR_DATA_TAG%s",
                            throwable.localizedMessage,
                        )
                    },
                ),
        )
    }


    private fun configureTerminal() {
        flutterEngine?.let {
            sendMessageToFlutter("terminalConfigured", mapOf("success" to true))
        }

        sendMessageToFlutter("showLoader", mapOf())

        compositeDisposable.add(
            netposPaymentClient.init(this, Gson().toJson(userData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let { response ->
                        Timber.tag("terminal_data").d(Gson().toJson(response))
                        sendMessageToFlutter("dismissLoader", mapOf())
                        sendMessageToFlutter(
                            "updateStatus",
                            mapOf("message" to getString(R.string.terminal_configured))
                        )
                        val keyHolder = response.first
                        val configData = response.second
                        val pinKey = keyHolder?.clearPinKey
                        if (pinKey != null) {
                            Prefs.putString(KEY_HOLDER, gson.toJson(keyHolder))
                            Prefs.putString(CONFIG_DATA, gson.toJson(configData))
                        }
                    }
                    error?.let {
                        sendMessageToFlutter("dismissLoader", mapOf())
                        sendMessageToFlutter(
                            "showError",
                            mapOf("message" to getString(R.string.terminal_config_failed))
                        )
                        Timber.d("%s%s", ERROR_TAG, it.localizedMessage)
                    }
                },
        )
    }

    private fun launchContactless(
        launcher: ActivityResultLauncher<Intent>,
        amountToPay: Double,
        cashBackAmount: Double = 0.0,
    ) {
        val savedKeyHolder = getSavedKeyHolder()

        savedKeyHolder?.run {
            ContactlessSdk.readContactlessCard(
                this@MainActivity,
                launcher,
                this.clearPinKey,
                amountToPay, // amount
                cashBackAmount, // cashBackAmount (optional)
            )
        } ?: run {
            // Instead of showing a Toast, we send a message to Flutter
            sendMessageToFlutter(
                "showError",
                mapOf("message" to getString(R.string.terminal_not_configured))
            )
            configureTerminal()
        }
    }

    private fun sendResultToFlutter(result: MethodChannel.Result, success: Boolean, data: String) {
        if (success) {
            result.success(data)
        } else {
            result.error("ERROR_CODE", data, null)
        }
    }

    private fun sendMessageToFlutter(method: String, arguments: Map<String, Any?>) {
        MethodChannel(flutterEngine?.dartExecutor!!.binaryMessenger, CHANNEL).invokeMethod(
            method,
            arguments
        )
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

}

