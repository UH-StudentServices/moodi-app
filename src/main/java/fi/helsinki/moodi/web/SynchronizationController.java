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

import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/synchronization")
public class SynchronizationController {

    private final SynchronizationJobRunService synchronizationJobRunService;
    private final SynchronizationService synchronizationService;

    @Autowired
    public SynchronizationController(SynchronizationJobRunService synchronizationJobRunService,
                                     SynchronizationService synchronizationService) {
        this.synchronizationJobRunService = synchronizationJobRunService;
        this.synchronizationService = synchronizationService;
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<Result<Boolean, Boolean>> getStatus(
        @RequestParam("type") final SynchronizationType type) {

        SynchronizationStatus status = synchronizationJobRunService.findLatestJob(type)
            .map(j -> j.status)
            .orElse(SynchronizationStatus.COMPLETED_SUCCESS);

        if(status != SynchronizationStatus.COMPLETED_FAILURE) {
            return response(Result.success(true));
        } else {
            return response(Result.error(true, "Error", "Last synchronization failed"));
        }
    }

    @RequestMapping(value="/unlock", method = RequestMethod.POST)
    @ResponseBody
    public SynchronizationSummary synchronizeAndUnlockLockedCourses() {
        return synchronizationService.synchronize(SynchronizationType.UNLOCK);
    }

    private <D, E> ResponseEntity<Result<D, E>> response(Result<D, E> result) {
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
