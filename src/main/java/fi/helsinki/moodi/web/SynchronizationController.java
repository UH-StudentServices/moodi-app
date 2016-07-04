package fi.helsinki.moodi.web;

import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.synchronize.job.SynchronizationJobRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/synchronization")
public class SynchronizationController {

    private final SynchronizationJobRunService synchronizationJobRunService;

    @Autowired
    public SynchronizationController(SynchronizationJobRunService synchronizationJobRunService) {
        this.synchronizationJobRunService = synchronizationJobRunService;
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

    private <D, E> ResponseEntity<Result<D, E>> response(Result<D, E> result) {
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
