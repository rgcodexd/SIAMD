package com.ogpika.siama.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ogpika.siama.data.local.entity.HolidayEntity
import com.ogpika.siama.data.local.entity.LeaveApplicationEntity
import com.ogpika.siama.data.local.entity.LeaveBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holiday_calendar ORDER BY holidayDate ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holiday_calendar WHERE category = :category ORDER BY holidayDate ASC")
    fun getHolidaysByCategory(category: String): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_balances WHERE internId = :internId LIMIT 1")
    fun getLeaveBalance(internId: String): Flow<LeaveBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaveBalance(balance: LeaveBalanceEntity)

    @Query("SELECT * FROM leave_applications WHERE internId = :internId ORDER BY appliedDate DESC")
    fun getInternApplications(internId: String): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications ORDER BY appliedDate DESC")
    fun getAllApplications(): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications WHERE status = 'PENDING' ORDER BY appliedDate ASC")
    fun getPendingApplications(): Flow<List<LeaveApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: LeaveApplicationEntity)

    @Update
    suspend fun updateApplication(application: LeaveApplicationEntity)
}
