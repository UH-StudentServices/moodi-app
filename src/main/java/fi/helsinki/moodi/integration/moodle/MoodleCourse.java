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

package fi.helsinki.moodi.integration.moodle;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public final class MoodleCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String idnumber;
    public final String fullName;
    public final String shortName;
    public final String categoryId;
    public final String summary;
    public final String format;
    public final boolean showGrades;
    public final boolean visible;
    public final boolean showReports;
    public final int maxBytes;
    public final int newsItems;
    public final int numSections;

    public MoodleCourse(
            String idnumber,
            String fullName,
            String shortName,
            String categoryId,
            String summary,
            String format,
            boolean showGrades,
            boolean visible,
            boolean showReports,
            int maxBytes,
            int newsItems,
            int numSections) {

        this.idnumber = idnumber;
        this.fullName = fullName;
        this.shortName = shortName;
        this.categoryId = categoryId;
        this.summary = summary;
        this.format = format;
        this.maxBytes = maxBytes;
        this.showGrades = showGrades;
        this.visible = visible;
        this.newsItems = newsItems;
        this.numSections = numSections;
        this.showReports = showReports;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
