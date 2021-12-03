/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.helsinki.moodi.test.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static fi.helsinki.moodi.util.DateFormat.UTC_DATE_FORMAT;

public class DateUtil {

    public static String formatDate(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern(UTC_DATE_FORMAT));
    }

    public static String getFutureDateString() {
        return formatDate(LocalDateTime.now().plusDays(1));
    }

    public static String getPastDateString() {
        return formatDate(LocalDateTime.now().minusDays(1));
    }

    public static String getOverYearAgoPastDateString() {
        return formatDate(LocalDateTime.now().minusDays(1).minusMonths(1).minusYears(1));
    }
}
