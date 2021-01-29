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

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;

/**
 * Resolves which {@link Action} to perform for {@link SynchronizationItem}
 * after enrichment phase.
 *
 * @see Action
 * @see SynchronizationItem
 */
public final class ActionResolver {

    public static Action resolve(final SynchronizationItem item) {

        switch (item.getEnrichmentStatus()) {
            case SUCCESS:
                return Action.SYNCHRONIZE;
            case COURSE_NOT_PUBLIC:
            case COURSE_ENDED:
            case MOODLE_COURSE_NOT_FOUND:
                return Action.REMOVE;
            case LOCKED:
            default:
                return Action.SKIP;
        }
    }
}
