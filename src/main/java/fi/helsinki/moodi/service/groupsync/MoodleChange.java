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

package fi.helsinki.moodi.service.groupsync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public class MoodleChange {
    @Setter private MoodleChangeStatus status = MoodleChangeStatus.NOT_APPLIED;
    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @NotNull
    private MoodleChangeType changeType;

    @JsonIgnore
    public boolean isMutation() {
        return Arrays.asList(
            MoodleChangeType.CREATE,
            MoodleChangeType.UPDATE,
            MoodleChangeType.DELETE
        ).contains(changeType);
    }

    protected MoodleChange(MoodleChangeType changeType) {
        this.changeType = changeType;
    }

    public boolean hasChanges() {
        return changeType != MoodleChangeType.KEEP;
    }
}
