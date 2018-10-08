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

    private final Map<UserSynchronizationActionType, SyncLimit> limits;

    private final Environment environment;

    @Autowired
    public SynchronizationThreshold(Environment environment) {
        this.environment = environment;

        limits = newArrayList(UserSynchronizationActionType.values()).stream()
            .collect(Collectors.toMap(Function.identity(),
                actionType -> new SyncLimit(
                    getProperty(actionType, THRESHOLD_LIMIT, Long.class),
                    getProperty(actionType, TRESHOLD_PREVENT_ALL, Long.class)
                )));
    }

    private <T> T getProperty(UserSynchronizationActionType actionType, String keySuffix, Class<T> targetType) {
        return environment.getProperty(THRESHOLD_PREFIX + "." + actionType + "." + keySuffix, targetType);
    }

    public boolean isLimitedByThreshold(UserSynchronizationActionType actionType, Long itemCount) {
        Long limit = limits.get(actionType).getLimit();

        return limit != null && limit <= itemCount;
    }

    public boolean isActionPreventedToAllItems(UserSynchronizationActionType actionType, Long itemCount) {
        Long preventAll = limits.get(actionType).getPreventAll();

        return preventAll != null &&
            preventAll > 0 &&
            preventAll <= itemCount;
    }

    private static class SyncLimit {
        private final Long limit;
        private final Long preventAll;

        private SyncLimit(Long limit, Long preventAll) {
            this.limit = limit;
            this.preventAll = preventAll;
        }

        public Long getLimit() {
            return limit;
        }

        public Long getPreventAll() {
            return preventAll;
        }
    }
}
