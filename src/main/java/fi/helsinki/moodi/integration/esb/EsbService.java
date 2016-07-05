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

package fi.helsinki.moodi.integration.esb;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class EsbService {

    private static final Logger LOGGER = getLogger(EsbService.class);

    private final EsbClient esbClient;

    @Autowired
    public EsbService(EsbClient esbClient) {
        this.esbClient = esbClient;
    }

    public Optional<String> getStudentUsername(final String studentNumber) {
        return Optional
            .ofNullable(esbClient.getStudentUsername(studentNumber))
            .map(this::appendDomain);
    }

    public Optional<String> getTeacherUsername(final String teacherId) {
        final String normalizedTeacherId = "9" + teacherId;
        return Optional
            .ofNullable(esbClient.getTeacherUsername(normalizedTeacherId))
            .map(this::appendDomain);
    }

    private String appendDomain(final String username) {
        return username + "@helsinki.fi";
    }
}
