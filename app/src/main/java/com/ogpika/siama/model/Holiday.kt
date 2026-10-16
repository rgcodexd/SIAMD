package com.ogpika.siama.model

import java.time.LocalDate

enum class HolidayCategory(val displayName: String, val quotaCount: Int, val description: String) {
    NATIONAL_HOLIDAY(
        displayName = "National Holiday",
        quotaCount = 3,
        description = "Admissible 03 (Three) days: Republic Day, Independence Day, Mahatma Gandhi Jayanti"
    ),
    SAIL_FOUNDATION_DAY(
        displayName = "SAIL Foundation Day",
        quotaCount = 1,
        description = "Admissible 01 (One) closed holiday on 24th January"
    ),
    FESTIVAL_HOLIDAY(
        displayName = "Festival Holiday (FH)",
        quotaCount = 6,
        description = "Admissible 06 (Six) days: Holi, Eid-ul-Fitr, Dussehra, Diwali, Guru Nanak Jayanti, Christmas"
    ),
    AMBEDKAR_JAYANTI(
        displayName = "Ambedkar Jayanti",
        quotaCount = 1,
        description = "Admissible 01 (One) day following the declaration by the government"
    )
}

data class Holiday(
    val id: String,
    val title: String,
    val date: LocalDate,
    val category: HolidayCategory,
    val isClosedHoliday: Boolean = true,
    val description: String = ""
)
