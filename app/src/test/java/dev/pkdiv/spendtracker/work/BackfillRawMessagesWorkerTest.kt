package dev.pkdiv.spendtracker.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionEntity
import dev.pkdiv.spendtracker.data.db.TransactionSource
import dev.pkdiv.spendtracker.ingestion.sms.ResolvedSmsBody
import dev.pkdiv.spendtracker.ingestion.sms.SmsBodyResolver
import dev.pkdiv.spendtracker.parsing.TransactionCategory
import dev.pkdiv.spendtracker.parsing.TransactionDirection
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackfillRawMessagesWorkerTest {

    private class FakeResolver(private val bodies: Map<Long, String>) : SmsBodyResolver {
        val requested = mutableListOf<Long>()
        override suspend fun resolve(smsId: Long): ResolvedSmsBody? {
            requested += smsId
            return bodies[smsId]?.let { ResolvedSmsBody("HDFCBK", it, false) }
        }
    }

    private fun entity(smsId: Long?): TransactionEntity = TransactionEntity(
        amount = BigDecimal("100.00"),
        direction = TransactionDirection.DEBIT,
        merchant = "Swiggy",
        account = null,
        category = TransactionCategory.FOOD,
        timestamp = Instant.ofEpochMilli(1700000000000),
        rawMessageRef = "sms:$smsId",
        smsId = smsId,
        rawSender = null,
        rawBody = null,
        source = TransactionSource.SMS,
        receivedAt = 1700000000000,
    )

    @Test
    fun resolvesAvailableRows_leavesDeletedRowsNull() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.transactionDao()
        val resolvableId = dao.insert(entity(smsId = 1))
        val deletedId = dao.insert(entity(smsId = 2))
        dao.insert(entity(smsId = null))

        val resolver = FakeResolver(mapOf(1L to "Rs.100 debited"))
        val completed = RawMessageBackfill(dao, resolver).run(hasSmsPermission = true)

        assertTrue(completed)
        assertEquals(setOf(1L, 2L), resolver.requested.toSet())
        assertEquals("Rs.100 debited", dao.observeById(resolvableId).first()?.rawBody)
        assertNull(dao.observeById(deletedId).first()?.rawBody)
        db.close()
    }

    @Test
    fun permissionRevoked_returnsFalse_andResolvesNothing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SpendTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.transactionDao()
        dao.insert(entity(smsId = 1))

        val resolver = FakeResolver(mapOf(1L to "Rs.100 debited"))
        val completed = RawMessageBackfill(dao, resolver).run(hasSmsPermission = false)

        assertFalse(completed)
        assertTrue(resolver.requested.isEmpty())
        assertNull(dao.observeById(1).first()?.rawBody)
        db.close()
    }
}
