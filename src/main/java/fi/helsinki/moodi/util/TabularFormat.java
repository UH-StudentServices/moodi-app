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

package fi.helsinki.moodi.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.partition;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class TabularFormat {

    private final List<List<String>> rows;
    private final int columnCount;
    private final int paddingLeft;

    public static Builder builder(String... headers) {
        return new Builder(headers);
    }

    public TabularFormat(final int rowWidth, final int paddingLeft, final String... values) {
        this(partition(asList(values), rowWidth), paddingLeft);
    }

    public TabularFormat(final List<List<String>> rows, final int paddingLeft) {

        if (paddingLeft < 0) {
            throw new IllegalArgumentException("Padding left must be greater than 0");
        }

        this.paddingLeft = paddingLeft;

        if (rows == null || rows.size() == 0) {
            throw new IllegalArgumentException("Table can't be null or empty");
        }

        columnCount = rows.get(0).size();
        for (List<String> row : rows) {
            if (row.size() != columnCount) {
                throw new IllegalArgumentException("All table rows must have same number of columns");
            }
        }

        this.rows = rows;
    }

    @Override
    public String toString() {
        final int[] columnWidths = calculateColumnWidths();
        final String template = buildTemplate(columnWidths);
        return toString(template);
    }

    private String toString(final String template) {
        final StringBuilder sb = new StringBuilder();
        for (final List<String> row : rows) {
            sb.append(StringUtils.repeat(' ', paddingLeft)).append(String.format(template, row.toArray())).append('\n');
        }

        return sb.toString();
    }

    private String buildTemplate(int[] columnWidths) {
        final StringBuilder sb = new StringBuilder();
        for (final int columnWidth : columnWidths) {
            sb.append("%-").append(columnWidth).append("s   ");
        }

        return sb.toString();
    }

    private int[] calculateColumnWidths() {
        final int[] columnWidths = new int[columnCount];

        for (final List<String> row : rows) {
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {

                final String columnValue = firstNonNull(row.get(columnIndex), "");
                if (columnValue.length() > columnWidths[columnIndex]) {
                    columnWidths[columnIndex] = columnValue.length();
                }
            }
        }

        return columnWidths;
    }

    public static class Builder {

        private final List<List<String>> table = new ArrayList<>();
        private final List<String> headers;

        private Builder(String... headers) {
            this.headers = Arrays.asList(requireNonNull(headers));
            table.add(this.headers);
        }

        public Builder addRow(String... values) {
            table.add(Arrays.asList(values));
            return this;
        }

        public TabularFormat build() {
            return new TabularFormat(table, 0);
        }
    }
}