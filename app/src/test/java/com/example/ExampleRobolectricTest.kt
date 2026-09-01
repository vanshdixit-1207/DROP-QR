package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoEngine
import com.example.data.AppDatabase
import com.example.data.TransferDirection
import com.example.data.TransferEntity
import com.example.data.TransferRepository
import com.example.data.TransferStatus
import com.example.protocol.ContactPayload
import com.example.protocol.QRProtocolEngine
import com.example.protocol.TransferPayloadType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TransferRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransferRepository(database.transferDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DropQR", appName)
    }

    @Test
    fun `crypto engine encryption and decryption round trip`() {
        val originalData = "Secret confidential data for DropQR air-gapped transmission".toByteArray(Charsets.UTF_8)
        val key = CryptoEngine.deriveKey("session_test_key_123")

        val encrypted = CryptoEngine.encryptAesGcm(originalData, key)
        val decrypted = CryptoEngine.decryptAesGcm(encrypted, key)

        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun `crypto engine compression and decompression round trip`() {
        val originalText = "Repeating text for testing GZIP compression efficiency. ".repeat(20)
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val compressed = CryptoEngine.compressGzip(originalBytes)
        val decompressed = CryptoEngine.decompressGzip(compressed)

        assertTrue(compressed.size < originalBytes.size)
        assertArrayEquals(originalBytes, decompressed)
    }

    @Test
    fun `qr protocol text encoding and decoding round trip`() {
        val text = "Hello from DropQR! This is an offline test message."
        val textBytes = text.toByteArray(Charsets.UTF_8)

        val (manifest, frames) = QRProtocolEngine.createTransferFrames(
            type = TransferPayloadType.TEXT,
            title = "Test Message",
            payloadBytes = textBytes,
            chunkSizeBytes = 200,
            encrypt = true,
            compress = true
        )

        assertTrue(frames.isNotEmpty())
        assertEquals(frames.size, manifest.totalFrames)

        // Simulate receiver reading all frames
        val receivedMap = HashMap<Int, String>()
        frames.forEach { frame ->
            val rawWire = QRProtocolEngine.encodeFrame(frame)
            val decodedFrame = QRProtocolEngine.decodeFrame(rawWire)
            assertNotNull(decodedFrame)
            assertEquals(frame.frameIndex, decodedFrame!!.frameIndex)
            receivedMap[decodedFrame.frameIndex] = decodedFrame.payloadChunk
        }

        // Unpack receiver side
        val unpacked = QRProtocolEngine.unpackTransfer(
            transferId = manifest.transferId,
            receivedChunksMap = receivedMap,
            totalFrames = manifest.totalFrames,
            isEncrypted = true
        )

        val reconstructedText = String(unpacked.payloadBytes, Charsets.UTF_8)
        assertEquals(text, reconstructedText)
    }

    @Test
    fun `qr protocol contact vcard round trip`() {
        val contact = ContactPayload(
            name = "Ada Lovelace",
            phone = "+1 555 0199",
            email = "ada@computing.org",
            organization = "Pioneers",
            note = "First programmer"
        )
        val vcard = contact.toVCard()
        val parsed = ContactPayload.fromVCard(vcard)

        assertEquals("Ada Lovelace", parsed.name)
        assertEquals("+1 555 0199", parsed.phone)
        assertEquals("ada@computing.org", parsed.email)
    }

    @Test
    fun `room database transfer repository operations`() = runBlocking {
        val entity = TransferEntity(
            transferId = "TX999",
            direction = TransferDirection.SENT,
            payloadType = "TEXT",
            title = "Notes Document",
            subtitle = "1 frame (42 B)",
            sizeBytes = 42L,
            frameCount = 1,
            status = TransferStatus.COMPLETED,
            sha256Checksum = "abc123sha",
            isEncrypted = true,
            detailsJson = "Test details",
            timestamp = System.currentTimeMillis()
        )

        repository.saveTransfer(entity)

        val all = repository.allTransfers.first()
        assertEquals(1, all.size)
        assertEquals("TX999", all.first().transferId)
        assertEquals("Notes Document", all.first().title)

        val sent = repository.sentTransfers.first()
        assertEquals(1, sent.size)

        val received = repository.receivedTransfers.first()
        assertEquals(0, received.size)
    }
}
