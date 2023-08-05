/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3

internal actual object PlatformDateFormat {

    private val delegate = LegacyCalendarModelImpl()

    actual val firstDayOfWeek: Int
        get() = delegate.firstDayOfWeek

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {
        return delegate.formatWithPattern(utcTimeMillis, pattern, locale)
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        locale: CalendarLocale
    ): String {
        // Note: there is no equivalent in Java for Android's DateFormat.getBestDateTimePattern.
        // The JDK SimpleDateFormat expects a pattern, so the results will be "2023Jan7",
        // "2023January", etc. in case a skeleton holds an actual ICU skeleton and not a pattern.

        // TODO: support ICU skeleton on JVM
        // Maybe it will be supported in kotlinx.datetime in the future.
        // See https://github.com/Kotlin/kotlinx-datetime/pull/251
        return formatWithPattern(utcTimeMillis, skeleton, locale)
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {


        return delegate.parse(date, pattern)
    }

    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return delegate.getDateInputFormat(locale)
    }

    actual fun weekdayNames(locale: CalendarLocale): List<Pair<String, String>>? {
        return delegate.weekdayNames(locale)
    }
}