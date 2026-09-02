package com.example.protocol

enum class TransferPayloadType(val code: String, val displayName: String) {
    TEXT("TXT", "Text Note"),
    URL("URL", "Web Link"),
    CONTACT("VCF", "Contact Card"),
    FILE("FIL", "Single File"),
    MULTI_FILE("PKG", "File Package"),
    AUDIO("AUD", "Voice Memo"),
    CRYPTO("SIG", "Cold Wallet / Crypto");

    companion object {
        fun fromCode(code: String): TransferPayloadType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: FILE
        }
    }
}

data class MissingFramesRequest(
    val transferId: String,
    val missingIndices: List<Int>
) {
    fun toQrString(): String {
        return "DQR_REQ|$transferId|${missingIndices.joinToString(",")}"
    }

    companion object {
        fun fromQrString(qrText: String): MissingFramesRequest? {
            if (!qrText.startsWith("DQR_REQ|")) return null
            val parts = qrText.split("|", limit = 3)
            if (parts.size < 3) return null
            val transferId = parts[1].trim()
            val indices = parts[2].split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
            if (transferId.isEmpty() || indices.isEmpty()) return null
            return MissingFramesRequest(transferId, indices)
        }
    }
}

data class ContactPayload(
    val name: String,
    val phone: String = "",
    val email: String = "",
    val organization: String = "",
    val note: String = ""
) {
    fun toVCard(): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:$name")
            if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL:$email")
            if (organization.isNotBlank()) appendLine("ORG:$organization")
            if (note.isNotBlank()) appendLine("NOTE:$note")
            append("END:VCARD")
        }
    }

    companion object {
        fun fromVCard(vcard: String): ContactPayload {
            var name = ""
            var phone = ""
            var email = ""
            var org = ""
            var note = ""

            vcard.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("FN:", ignoreCase = true) -> name = trimmed.substring(3).trim()
                    trimmed.startsWith("TEL", ignoreCase = true) -> {
                        val idx = trimmed.indexOf(':')
                        if (idx != -1) phone = trimmed.substring(idx + 1).trim()
                    }
                    trimmed.startsWith("EMAIL", ignoreCase = true) -> {
                        val idx = trimmed.indexOf(':')
                        if (idx != -1) email = trimmed.substring(idx + 1).trim()
                    }
                    trimmed.startsWith("ORG:", ignoreCase = true) -> org = trimmed.substring(4).trim()
                    trimmed.startsWith("NOTE:", ignoreCase = true) -> note = trimmed.substring(5).trim()
                }
            }
            return ContactPayload(
                name = name.ifBlank { "Contact" },
                phone = phone,
                email = email,
                organization = org,
                note = note
            )
        }
    }
}

data class TransferFileItem(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val checksumSha256: String = ""
)

data class TransferManifest(
    val transferId: String,
    val type: TransferPayloadType,
    val title: String,
    val totalBytes: Long,
    val totalFrames: Int,
    val isEncrypted: Boolean,
    val isCompressed: Boolean,
    val overallSha256: String,
    val files: List<TransferFileItem> = emptyList(),
    val isPasswordProtected: Boolean = false,
    val passwordHint: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeLockUntil: Long = 0L
)

data class QRFrame(
    val version: String,
    val transferId: String,
    val type: TransferPayloadType,
    val frameIndex: Int, // 1-based index
    val totalFrames: Int,
    val crc32Hex: String,
    val payloadChunk: String
)
