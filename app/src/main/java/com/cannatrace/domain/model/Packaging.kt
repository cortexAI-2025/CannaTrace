package com.cannatrace.domain.model

data class Packaging(
    val id: String,
    val batchId: String,
    val labelNumber: String,
    val expiryDate: Long,
    val quantity: Double,
    val unit: String,
    val qrCodeData: String,
    val legalMentions: String,
    val packagedAt: Long,
    val operatorId: String,
    val notes: String = ""
)
