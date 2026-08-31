package com.kuronoa.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE bulan = :bulan ORDER BY tanggal DESC, localId DESC")
    fun observeByMonth(bulan: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY tanggal DESC, localId DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE bulan = :bulan")
    suspend fun getByMonth(bulan: String): List<ExpenseEntity>

    @Query("SELECT DISTINCT bulan FROM expenses WHERE bulan != ''")
    suspend fun getDistinctMonths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity): Long

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Query("UPDATE expenses SET pendingDelete = 1, dirty = 1 WHERE localId = :localId")
    suspend fun markPendingDelete(localId: Long)

    @Query("DELETE FROM expenses WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("SELECT * FROM expenses WHERE bulan = :bulan AND (dirty = 1 OR pendingDelete = 1 OR serverId = '')")
    suspend fun getPendingSync(bulan: String): List<ExpenseEntity>
}
