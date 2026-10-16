package com.ogpika.siama.data

import com.ogpika.siama.model.Holiday
import com.ogpika.siama.model.HolidayCategory
import java.time.LocalDate

object SailHolidayRepository {

    /**
     * Official SAIL Calendar 2025/2026 Admissible Holiday Schedule:
     * - 03 National Holidays (Republic Day, Independence Day, Mahatma Gandhi Jayanti)
     * - 01 SAIL Foundation Day (Closed Holiday, 24th January)
     * - 06 Festival Holidays (Holi, Eid-ul-Fitr, Dussehra, Diwali, Guru Nanak Jayanti, Christmas)
     * - 01 Ambedkar Jayanti (14th April - Govt declared)
     * Total: 11 Admissible Closed Holidays
     */
    val officialHolidays2025: List<Holiday> = listOf(
        // c. SAIL Foundation Day Closed Holiday :- 01 (one)
        Holiday(
            id = "sail-fd-2025",
            title = "SAIL Foundation Day",
            date = LocalDate.of(2025, 1, 24),
            category = HolidayCategory.SAIL_FOUNDATION_DAY,
            isClosedHoliday = true,
            description = "Closed Holiday on account of Steel Authority of India Limited Foundation Day"
        ),
        // b. National Holiday Leaves :- 03(Three) days (1/3)
        Holiday(
            id = "nh-rd-2025",
            title = "Republic Day",
            date = LocalDate.of(2025, 1, 26),
            category = HolidayCategory.NATIONAL_HOLIDAY,
            isClosedHoliday = true,
            description = "National Holiday - 76th Republic Day of India"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (1/6)
        Holiday(
            id = "fh-holi-2025",
            title = "Holi",
            date = LocalDate.of(2025, 3, 14),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Festival of Colours"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (2/6)
        Holiday(
            id = "fh-eid-2025",
            title = "Eid-ul-Fitr",
            date = LocalDate.of(2025, 3, 31),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Eid celebration"
        ),
        // e. Ambedkar Jayanti Holiday: - 01 (One) following the declaration by the government
        Holiday(
            id = "aj-2025",
            title = "Dr. B.R. Ambedkar Jayanti",
            date = LocalDate.of(2025, 4, 14),
            category = HolidayCategory.AMBEDKAR_JAYANTI,
            isClosedHoliday = true,
            description = "Closed Holiday following government declaration in honor of Dr. B.R. Ambedkar"
        ),
        // b. National Holiday Leaves :- 03(Three) days (2/3)
        Holiday(
            id = "nh-id-2025",
            title = "Independence Day",
            date = LocalDate.of(2025, 8, 15),
            category = HolidayCategory.NATIONAL_HOLIDAY,
            isClosedHoliday = true,
            description = "National Holiday - 79th Independence Day"
        ),
        // b. National Holiday Leaves :- 03(Three) days (3/3)
        Holiday(
            id = "nh-gj-2025",
            title = "Mahatma Gandhi Jayanti",
            date = LocalDate.of(2025, 10, 2),
            category = HolidayCategory.NATIONAL_HOLIDAY,
            isClosedHoliday = true,
            description = "National Holiday - Birthday of Mahatma Gandhi"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (3/6)
        Holiday(
            id = "fh-dussehra-2025",
            title = "Dussehra (Vijaya Dashami)",
            date = LocalDate.of(2025, 10, 2),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Vijaya Dashami"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (4/6)
        Holiday(
            id = "fh-diwali-2025",
            title = "Diwali (Deepawali)",
            date = LocalDate.of(2025, 10, 20),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Festival of Lights"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (5/6)
        Holiday(
            id = "fh-gnj-2025",
            title = "Guru Nanak Jayanti",
            date = LocalDate.of(2025, 11, 5),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Guru Nanak Gurpurab"
        ),
        // d. Festival Holidays (FHs) :- 06(Six) (6/6)
        Holiday(
            id = "fh-xmas-2025",
            title = "Christmas Day",
            date = LocalDate.of(2025, 12, 25),
            category = HolidayCategory.FESTIVAL_HOLIDAY,
            isClosedHoliday = true,
            description = "Festival Holiday - Christmas celebration"
        )
    )

    fun getHolidaysByCategory(category: HolidayCategory?): List<Holiday> {
        return if (category == null) {
            officialHolidays2025
        } else {
            officialHolidays2025.filter { it.category == category }
        }
    }

    fun isHoliday(date: LocalDate): Holiday? {
        return officialHolidays2025.firstOrNull { it.date == date }
    }
}
