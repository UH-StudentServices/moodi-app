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

package fi.helsinki.moodi.service.time;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SystemTimeService implements TimeService {

    @Override
    public LocalDateTime getCurrentUTCDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    @Override
    public long getCurrentUTCTimestamp() {
        return getCurrentUTCDateTime().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    @Override
    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
}
