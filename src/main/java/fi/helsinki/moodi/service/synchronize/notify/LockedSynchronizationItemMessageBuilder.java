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

import fi.helsinki.moodi.exception.NotifierException;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LockedSynchronizationItemMessageBuilder {
    private static final String LIST_DELIMITER = ", ";

    private final String from;
    private final String to;
    private final String subject;

    @Autowired
    public LockedSynchronizationItemMessageBuilder(Environment environment) {
        this.from = environment.getProperty("mailNotification.from");
        this.to = environment.getProperty("mailNotification.to");
        this.subject = environment.getProperty("mailNotification.lockedMessageSubject");
    }

    public SimpleMailMessage buildMessage(List<SynchronizationItem> lockedItems) {
        if(from != null && to != null && subject != null) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            mailMessage.setFrom(from);
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(lockedItems.stream()
                .map(i -> i.getCourse().realisationId)
                .map(String::valueOf)
                .collect(Collectors.joining(LIST_DELIMITER)));

            return mailMessage;
        } else {
            throw new NotifierException(String.format(
                "Could not build mail message (from: %s, to: %s, subject: %s)",
                from,
                to,
                subject));
        }
    }

}
