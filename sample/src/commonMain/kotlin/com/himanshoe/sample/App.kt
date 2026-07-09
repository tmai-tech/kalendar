/*
 *
 *  * Copyright 2026 Kalendar Contributors (https://www.himanshoe.com). All rights reserved.
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package com.himanshoe.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.himanshoe.kalendar.Kalendar
import com.himanshoe.kalendar.KalendarType
import com.himanshoe.kalendar.foundation.action.OnDaySelectionAction
import com.himanshoe.kalendar.foundation.color.asSolidColor
import com.himanshoe.kalendar.foundation.component.KalendarDay
import com.himanshoe.kalendar.foundation.component.config.KalendarConfig
import com.himanshoe.kalendar.foundation.component.config.KalendarDayConfig
import com.himanshoe.kalendar.foundation.component.config.KalendarDayLabelConfig
import com.himanshoe.kalendar.foundation.event.BasicKalendarEvent
import com.himanshoe.kalendar.foundation.event.KalendarEvents
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * Sample entry point: Monday-first month view with weekly recurring days highlighted
 * via [KalendarDayConfig.selectedBackgroundColor].
 */
@Composable
fun App() {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
    ) {
        Text(
            text = "Team standup — every Wednesday (Mon-first grid)",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        MondayMonthRecurringWeekKalendar(
            recurringWeekDays = setOf(DayOfWeek.WEDNESDAY),
            eventName = "Team standup",
            highlightColor = Color(0xFF5B8DEF),
        )
    }
}

/**
 * Oceanic (month) calendar that:
 * 1. Starts the week grid on **Monday** via [KalendarConfig.startDayOfWeek].
 * 2. Aligns weekday column labels to that same Monday-first order.
 * 3. Highlights every date on [recurringWeekDays] using
 *    [KalendarDayConfig.selectedBackgroundColor] ([highlightColor]).
 * 4. Expands the weekly series across **grid pad** days so a series day that falls in the
 *    previous calendar month (first row Monday / mid-week before the 1st) still highlights.
 *
 * @param recurringWeekDays Weekdays in the weekly series (e.g. every Wednesday).
 * @param eventName Title for generated events.
 * @param highlightColor Circle fill from [KalendarDayConfig.selectedBackgroundColor].
 * @param seriesYear Calendar year used as the logical series window (expanded for pad days).
 * @param modifier Modifier for the calendar.
 */
@Composable
fun MondayMonthRecurringWeekKalendar(
    recurringWeekDays: Set<DayOfWeek>,
    eventName: String,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    seriesYear: Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedDate by remember { mutableStateOf(today) }
    val weekStart = DayOfWeek.MONDAY
    val isDateBeforeToday: (LocalDate) -> Boolean = remember(today) {
        { date -> date < today }
    }

    val expansionRange = remember(seriesYear, weekStart) {
        seriesExpansionRangeForYear(seriesYear, weekStart)
    }

    val recurringDates = remember(recurringWeekDays, expansionRange) {
        expandWeeklyRecurrence(
            weekDays = recurringWeekDays,
            rangeStart = expansionRange.start,
            rangeEnd = expansionRange.endInclusive,
        )
    }
    val recurringDateSet = remember(recurringDates) { recurringDates.toSet() }

    val events: KalendarEvents = remember(recurringDates, eventName, highlightColor) {
        recurringDates.map { date ->
            BasicKalendarEvent(
                date = date,
                eventName = eventName,
                eventDescription = "Weekly recurring",
                eventColor = highlightColor,
            )
        }
    }

    val dayConfig = remember(highlightColor) {
        KalendarDayConfig(
            selectedBackgroundColor = highlightColor.asSolidColor(),
            selectedTextColor = Color.White.asSolidColor(),
            indicatorColor = highlightColor.asSolidColor(),
            borderColor = highlightColor.asSolidColor(),
            textStyle = TextStyle(
                fontSize = 16.sp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF413D4B), Color(0xFF413D4B)),
                ),
            ),
        )
    }

    val dayLabelConfig = remember {
        KalendarDayLabelConfig.default().copy(
            dayNameFormatter = { day -> mondayFirstShortLabel(day) },
        )
    }

    Kalendar(
        type = KalendarType.Oceanic,
        selectedDate = selectedDate,
        modifier = modifier.fillMaxWidth(),
        events = events,
        onDaySelectionAction = OnDaySelectionAction.NoOp,
        config = KalendarConfig(
            startDayOfWeek = weekStart,
            dayConfig = dayConfig,
            dayLabelConfig = dayLabelConfig,
            minDate = today,
            disabledDates = isDateBeforeToday,
            onVisibleRangeChange = { start, end ->
                println("Visible grid (includes pad): $start → $end")
            },
        ),
        dayContent = { date, isSelected, dayEvents ->
            val isRecurringDay = date in recurringDateSet
            val isDisabled = isDateBeforeToday(date)
            KalendarDay(
                date = date,
                selectedDate = if (isSelected) date else selectedDate,
                selectedDates = if (isRecurringDay && !isDisabled) listOf(date) else emptyList(),
                events = dayEvents,
                dayConfig = dayConfig,
                isDisabled = isDisabled,
                onDayClick = { clicked, clickedEvents ->
                    if (!isDateBeforeToday(clicked)) {
                        selectedDate = clicked
                        println("Tapped $clicked — events: ${clickedEvents.map { it.eventName }}")
                    }
                },
            )
        },
    )
}

/**
 * Inclusive date range for expanding a weekly series that must paint every cell of every
 * Monday-first month grid in [year].
 *
 * January's first grid week often starts in December of [year] - 1. December's last grid week
 * often ends in January of [year] + 1. Naïve Jan 1…Dec 31 expansion drops those pad cells —
 * that is the month-transition edge case (first Monday of the displayed January week lands in
 * the previous calendar month/year).
 */
internal fun seriesExpansionRangeForYear(
    year: Int,
    startDayOfWeek: DayOfWeek,
): ClosedRange<LocalDate> {
    val januaryFirst = LocalDate(year, 1, 1)
    val decemberLast = LocalDate(year, 12, 31)
    val gridStart = startOfWeekContaining(januaryFirst, startDayOfWeek)
    val gridEnd = startOfWeekContaining(decemberLast, startDayOfWeek)
        .plus(6, DateTimeUnit.DAY)
    return gridStart..gridEnd
}

/**
 * First day of the week containing [date] for [startDayOfWeek].
 * May fall in the previous calendar month (and previous year).
 */
internal fun startOfWeekContaining(
    date: LocalDate,
    startDayOfWeek: DayOfWeek,
): LocalDate {
    val daysBack = (date.dayOfWeek.ordinal - startDayOfWeek.ordinal + 7) % 7
    return date.minus(daysBack, DateTimeUnit.DAY)
}

/**
 * Every [LocalDate] in [[rangeStart], [rangeEnd]] whose weekday is in [weekDays].
 */
internal fun expandWeeklyRecurrence(
    weekDays: Set<DayOfWeek>,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
): List<LocalDate> {
    if (weekDays.isEmpty() || rangeStart > rangeEnd) return emptyList()

    val dates = mutableListOf<LocalDate>()
    var cursor = rangeStart
    while (cursor <= rangeEnd) {
        if (cursor.dayOfWeek in weekDays) {
            dates += cursor
        }
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return dates
}

/**
 * Short weekday labels ordered for a Monday-first grid (en-GB style), independent of JVM locale.
 * Must stay in sync with [KalendarConfig.startDayOfWeek] = [DayOfWeek.MONDAY].
 */
internal fun mondayFirstShortLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}
