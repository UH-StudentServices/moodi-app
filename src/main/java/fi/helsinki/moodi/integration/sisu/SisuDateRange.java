package fi.helsinki.moodi.integration.sisu;

import java.time.LocalDate;

/**
 * A semi-open interval, start inclusive, end exclusive.
 */
public class SisuDateRange {

    public LocalDate startDate;
    public LocalDate endDate;

    public SisuDateRange() {}
    public SisuDateRange(LocalDate startDate, LocalDate endDate) {
        startDate = startDate;
        endDate = endDate;
    }
}
