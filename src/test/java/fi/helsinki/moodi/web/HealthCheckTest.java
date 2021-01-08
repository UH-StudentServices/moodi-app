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

package fi.helsinki.moodi.web;

import fi.helsinki.moodi.MoodiHealthIndicator;
import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportCourseResponse;
import fi.helsinki.moodi.service.importing.ImportingService;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRun;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunRepository;
import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthCheckTest extends AbstractMoodiIntegrationTest {
    private static String EXCEPTION_MESSAGE = "It's all gone Pete Tong";

    @MockBean
    private TimeService timeService;

    @MockBean
    private SynchronizationJobRunRepository synchronizationJobRunRepository;

    @MockBean
    private ImportingService importingService;

    @Autowired
    private MoodiHealthIndicator moodiHealthIndicator;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        moodiHealthIndicator.clearError();
    }

    @Test
    public void thatReturnsOkWhenJobWasRunWithinLastThreeHours() throws Exception {
        setupJobRunHoursAgo(1);

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void thatReturnsOkWhenCourseImportFailedFortyMinutesAgo() throws Exception {
        setupJobRunHoursAgo(1);

        setTime(-40);
        when(importingService.importCourse(any())).thenThrow(new NullPointerException(EXCEPTION_MESSAGE));
        makeCreateCourseRequest("123").andExpect(status().is5xxServerError());

        setTime(0);

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void thatReturnsOkWhenCourseImportFailedTenMinutesAgoButSucceededAfterThat() throws Exception {
        setupJobRunHoursAgo(1);

        setTime(-10);
        when(importingService.importCourse(new ImportCourseRequest("123"))).thenThrow(new NullPointerException(EXCEPTION_MESSAGE));
        makeCreateCourseRequest("123").andExpect(status().is5xxServerError());

        setTime(0);

        when(importingService.importCourse(new ImportCourseRequest("456"))).thenReturn(Result.success(new ImportCourseResponse(456)));
        makeCreateCourseRequest("456").andExpect(status().isOk());

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void thatReturnsErrorWhenNoJobCompletedInThreeHours() throws Exception {
        LocalDateTime fourHoursAgo = setupJobRunHoursAgo(4);

        mockMvc.perform(get("/health"))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.details.moodi.details.error")
                .value(String.format("No job completed in 3 hours. Latest job completed at %s UTC", fourHoursAgo)));
    }

    @Test
    public void thatReturnsErrorWhenNoJobFound() throws Exception {
        setupLastRun(Optional.empty());
        setTime(0);

        mockMvc.perform(get("/health"))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.details.moodi.details.error").value("No sync jobs in the DB."));
    }

    @Test
    public void thatReturnsErrorWhenCourseImportFailedJustNow() throws Exception {
        setupJobRunHoursAgo(1);

        when(importingService.importCourse(any())).thenThrow(new NullPointerException(EXCEPTION_MESSAGE));

        makeCreateCourseRequest("123").andExpect(status().is5xxServerError());

        mockMvc.perform(get("/health"))
            .andDo(print())
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.details.moodi.details.error").value("java.lang.NullPointerException: " + EXCEPTION_MESSAGE))
            .andExpect(jsonPath("$.details.moodi.details.stack").hasJsonPath());
    }

    private LocalDateTime setupJobRunHoursAgo(int hours) {
        LocalDateTime hoursAgo = LocalDateTime.now(ZoneOffset.UTC).minusHours(hours);
        setupLastRun(getRun(hoursAgo));

        setTime(0);
        return hoursAgo;
    }

    private void setTime(int diffMinutes) {
        when(timeService.getCurrentUTCDateTime()).thenReturn(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(diffMinutes));
    }

    private void setupLastRun(Optional<SynchronizationJobRun> jobRun) {
        when(synchronizationJobRunRepository.findFirstByStatusInOrderByCompletedDesc(anyList())).thenReturn(jobRun);
    }

    private Optional<SynchronizationJobRun> getRun(LocalDateTime completed) {
        SynchronizationJobRun ret = new SynchronizationJobRun();
        ret.completed = completed;
        return Optional.of(ret);
    }
}
