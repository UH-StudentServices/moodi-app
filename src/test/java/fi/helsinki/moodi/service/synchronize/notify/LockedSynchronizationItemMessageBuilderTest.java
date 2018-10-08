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

package fi.helsinki.moodi.service.synchronize.notify;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;

public class LockedSynchronizationItemMessageBuilderTest extends AbstractMoodiIntegrationTest {

    private static final Long REALISATION_ID_1 = 1L;
    private static final Long REALISATION_ID_2 = 2L;
    private static final String EXPECTED_MESSAGE = REALISATION_ID_1 + ", " + REALISATION_ID_2;

    @Autowired
    private LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder;

    @Test
    public void thatMessageIsBuiltCorrectly() {
        List<SynchronizationItem> synchronizationItems = newArrayList(createItem(REALISATION_ID_1), createItem(REALISATION_ID_2));

        SimpleMailMessage message = lockedSynchronizationItemMessageBuilder.buildMessage(synchronizationItems);

        assertEquals(EXPECTED_MESSAGE, message.getText());
    }

    private SynchronizationItem createItem(Long realisationId) {
        Course course = new Course();
        course.realisationId = realisationId;

        return new SynchronizationItem(course, SynchronizationType.FULL);
    }
}
