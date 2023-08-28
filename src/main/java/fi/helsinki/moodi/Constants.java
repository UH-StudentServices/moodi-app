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

package fi.helsinki.moodi;

import fi.helsinki.moodi.integration.sisu.SisuLocalisedValue;

import java.util.Arrays;
import java.util.List;

public final class Constants {

    public static final String LANG_FI = "fi";
    public static final String LANG_SV = "sv";
    public static final String LANG_EN = "en";
    public static final String LANG_DEFAULT = LANG_FI;
    public static final String ACTIVE = "ACTIVE";
    public static final String RESPONSIBLE_ORGANISATION = "urn:code:organisation-role:responsible-organisation";
    public static final String CUR_RESP_TYPE = "urn:code:course-unit-realisation-responsibility-info-type:";
    public static final String RESPONSIBLE_TEACHER = CUR_RESP_TYPE + "responsible-teacher";
    public static final String TEACHER = CUR_RESP_TYPE + "teacher";
    public static final List<String> TEACHER_TYPES = Arrays.asList(RESPONSIBLE_TEACHER, TEACHER);
    public static final String REALISATION_TYPE_FI = "Kurssi";
    public static final String REALISATION_TYPE_SV = "Kurs";
    public static final String REALISATION_TYPE_EN = "Course";

    public static final String MOODLE_SISU_COMMON_GROUPING_ID = "sisu-synchronised";
    public static final String MOODLE_SISU_COMMON_GROUPING_NAME_FI = "Sisusta synkronoidut";
    public static final String MOODLE_SISU_COMMON_GROUPING_NAME_SV = "Synkroniserade från Sisu";
    public static final String MOODLE_SISU_COMMON_GROUPING_NAME_EN = "Synchronized from Sisu";

    public static final SisuLocalisedValue SISU_COMMON_GROUPING_NAME = new SisuLocalisedValue(
        MOODLE_SISU_COMMON_GROUPING_NAME_FI,
        MOODLE_SISU_COMMON_GROUPING_NAME_SV,
        MOODLE_SISU_COMMON_GROUPING_NAME_EN
    );
    public static final String MOODLE_SISU_ID_PREFIX = "sisu:";
    public static final Long MOODLE_DESCRIPTION_FORMAT_AUTO = 0L;
    public static final Long MOODLE_DESCRIPTION_FORMAT_HTML = 1L;
    public static final Long MOODLE_DESCRIPTION_FORMAT_PLAIN = 2L;
    public static final Long MOODLE_DESCRIPTION_FORMAT_MARKDOWN = 4L;

    private Constants() {
    }
}
