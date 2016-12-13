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

import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.util.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SynchronizationActionResolver {

    private final MapperService mapperService;

    @Autowired
    public SynchronizationActionResolver(MapperService mapperService) {
        this.mapperService = mapperService;
    }

    private boolean isAddRole(MoodleUserEnrollments moodleUserEnrollments, long moodleRoleId, boolean approved) {
        return moodleUserEnrollments != null
            && approved
            && !moodleUserEnrollments.hasRole(moodleRoleId);
    }

    private boolean isAddSynced(MoodleUserEnrollments moodleUserEnrollments, long moodleRoleId) {
        return moodleUserEnrollments != null
            && moodleUserEnrollments.hasRole(moodleRoleId)
            && !moodleUserEnrollments.hasRole(mapperService.getMoodiRoleId());
    }

    private boolean isAddEnrollment(MoodleUserEnrollments moodleUserEnrollments, boolean approved) {
        return moodleUserEnrollments == null
            && approved;
    }

    private boolean isRemoveRole(MoodleUserEnrollments moodleUserEnrollments, long moodleRoleId, boolean approved) {
        return moodleUserEnrollments != null
            && !approved
            && moodleUserEnrollments.hasRole(moodleRoleId);
    }

    public SynchronizationAction resolveSynchronizationAction(final EnrollmentSynchronizationItem item) {
        final long moodleRoleId = item.getMoodleRoleId();
        final MoodleUserEnrollments moodleUserEnrollments = item.getMoodleEnrollments().orElse(null);
        final boolean approved = item.isApproved();

        if (isAddEnrollment(moodleUserEnrollments, approved)) {
            return SynchronizationAction.ADD_ENROLLMENT_WITH_MOODI_ROLE;
        } else if(isRemoveRole(moodleUserEnrollments, moodleRoleId, approved)) {
            return SynchronizationAction.REMOVE_ROLE;
        } else if(isAddRole(moodleUserEnrollments, moodleRoleId, approved)) {
            return SynchronizationAction.ADD_ROLE;
        } else if(isAddSynced(moodleUserEnrollments, moodleRoleId)) {
            return SynchronizationAction.ADD_MOODI_ROLE;
        }

        return SynchronizationAction.NONE;
    }
}
