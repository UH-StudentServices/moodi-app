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

import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.ProcessingStatus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class LockedSynchronizationItemNotifier implements SynchronizationItemNotifier {

    private static final Logger LOGGER = getLogger(LockedSynchronizationItemNotifier.class);

    private final MailSender mailSender;
    private final LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder;

    @Autowired
    public LockedSynchronizationItemNotifier(MailSender mailSender,
                                             LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder) {
        this.mailSender = mailSender;
        this.lockedSynchronizationItemMessageBuilder = lockedSynchronizationItemMessageBuilder;
    }

    @Override
    public void applyNotificationsForItems(List<SynchronizationItem> synchronizationItems) {

        List<SynchronizationItem> lockedItems = synchronizationItems
            .stream()
            .filter(i -> i.getProcessingStatus().equals(ProcessingStatus.LOCKED))
            .collect(Collectors.toList());

        if (lockedItems.size() > 0) {
            formatAndSendNotification(lockedItems);
        }
    }

    private void formatAndSendNotification(List<SynchronizationItem> lockedItems) {
        try {
            SimpleMailMessage mailMessage = lockedSynchronizationItemMessageBuilder.buildMessage(lockedItems);
            mailSender.send(mailMessage);

            LOGGER.info("Sent mail notification for locked items:");
            LOGGER.info(mailMessage.toString());
        } catch (Exception e) {
            LOGGER.error("Error when sending locked notification email", e);
        }
    }
}
