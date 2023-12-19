package com.netplus.netpos_contactless_flutter_sample.model

import com.netpluspay.contactless.sdk.card.CardReadResult

data class CardResult(
    val cardReadResult: CardReadResult,
    val cardScheme: String,
)

