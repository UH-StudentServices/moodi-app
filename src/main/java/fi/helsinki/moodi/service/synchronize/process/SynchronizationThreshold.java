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

package fi.helsinki.moodi.service.synchronize.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class SynchronizationThreshold {

    private static final String THRESHOLD_PREFIX = "syncTresholds";
    private static final String THRESHOLD_LIMIT = "limit";
    private static final String TRESHOLD_PREVENT_ALL = "preventAll";

    private final Map<SynchronizationAction, SyncLimit> limits;

    @Autowired
    public SynchronizationThreshold(Environment environment) {
        limits = newArrayList(SynchronizationAction.values()).stream()
            .collect(Collectors.toMap(Function.identity(),
                action -> new SyncLimit(
                        environment.getProperty(THRESHOLD_PREFIX + "." + action + "." + THRESHOLD_LIMIT, Long.class),
                        environment.getProperty(THRESHOLD_PREFIX + "." + action + "." + TRESHOLD_PREVENT_ALL, Boolean.class))
                ));


    }

    public boolean isLimitedByThreshold(SynchronizationAction action, Long itemCount) {
       Long limit = limits.get(action).getLimit();

       return limit != null && limit <= itemCount;
    }

    public boolean isActionPreventedToAllItems(SynchronizationAction action) {
       Boolean isPreventAll = limits.get(action).isPreventAll();

       return isPreventAll != null && isPreventAll;
    }

    private static class SyncLimit {
        private Long limit;
        private Boolean preventAll;

        private SyncLimit(Long limit, Boolean preventAll) {
            this.limit = limit;
            this.preventAll = preventAll;
        }

        public Long getLimit() {
            return limit;
        }

        public Boolean isPreventAll() {
            return preventAll;
        }
    }
}
