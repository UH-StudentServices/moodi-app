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

package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static fi.helsinki.moodi.service.synchronize.SynchronizationType.DELTA;

/**
 * Scheduled job that performs the delta-based synchronization
 * of all Moodle courses created by Moodi.
 */
@Component
public class DeltaSynchronizationJob {

    private final Environment environment;
    private final SynchronizationService synchronizationService;

    @Autowired
    public DeltaSynchronizationJob(Environment environment, SynchronizationService synchronizationService) {
        this.environment = environment;
        this.synchronizationService = synchronizationService;
    }

    // Run 30 minutes after the previous run has completed
    @Scheduled(initialDelay = 60000, fixedDelay = 1800000)
    public void execute() {
        if (isEnabled()) {
            synchronizationService.synchronize(DELTA);
        }
    }

    public boolean isEnabled() {
        final String key = String.format("synchronize.%s.enabled", DELTA);
        return environment.getRequiredProperty(key, Boolean.class);
    }
}
