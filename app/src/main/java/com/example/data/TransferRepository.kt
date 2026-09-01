package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransferRepository(private val transferDao: TransferDao) {

    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()
    val sentTransfers: Flow<List<TransferEntity>> = transferDao.getTransfersByDirection(TransferDirection.SENT)
    val receivedTransfers: Flow<List<TransferEntity>> = transferDao.getTransfersByDirection(TransferDirection.RECEIVED)

    suspend fun getTransferById(id: Long): TransferEntity? = withContext(Dispatchers.IO) {
        transferDao.getTransferById(id)
    }

    suspend fun saveTransfer(transfer: TransferEntity): Long = withContext(Dispatchers.IO) {
        transferDao.insertTransfer(transfer)
    }

    suspend fun updateTransfer(transfer: TransferEntity) = withContext(Dispatchers.IO) {
        transferDao.updateTransfer(transfer)
    }

    suspend fun deleteTransfer(id: Long) = withContext(Dispatchers.IO) {
        transferDao.deleteTransferById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        transferDao.clearAll()
    }
}
