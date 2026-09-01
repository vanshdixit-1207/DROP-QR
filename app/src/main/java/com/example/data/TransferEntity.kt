package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransferDirection {
    SENT,
    RECEIVED
}

enum class TransferStatus {
    COMPLETED,
    CANCELLED,
    INTERRUPTED,
    FAILED
}

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transferId: String,
    val direction: TransferDirection,
    val payloadType: String,
    val title: String,
    val subtitle: String = "",
    val sizeBytes: Long = 0L,
    val frameCount: Int = 1,
    val status: TransferStatus = TransferStatus.COMPLETED,
    val filePath: String? = null,
    val sha256Checksum: String = "",
    val isEncrypted: Boolean = true,
    val detailsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
