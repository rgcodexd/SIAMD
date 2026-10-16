package com.ogpika.siama.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ogpika.siama.model.HolidayCategory

@Entity(tableName = "holiday_calendar")
data class HolidayEntity(
    @PrimaryKey
    val id: String,
    val calendarYear: Int,
    val title: String,
    val holidayDate: String, // ISO-8601 YYYY-MM-DD
    val category: String,    // NATIONAL_HOLIDAY, SAIL_FOUNDATION_DAY, FESTIVAL_HOLIDAY, AMBEDKAR_JAYANTI
    val isClosedHoliday: Boolean = true,
    val description: String = ""
)
