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

package fi.helsinki.moodi.service.util;

import fi.helsinki.moodi.integration.sisu.SisuPerson;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

// For development purposes: generates fake usernames for Sisu persons when running
// against a development Moodle (MODE=dev).
public class DevModeUtils {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";

    private static final boolean DEV_MODE = System.getenv("MODE") != null && System.getenv("MODE").equals("dev");

    public static String pseudoRandomId(int length, String seedStr) {
        StringBuilder password = new StringBuilder(length);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(seedStr.getBytes());

        byte[] hashBytes = md.digest();

        // Use only the first 8 bytes to get a long
        long seed = ByteBuffer.wrap(hashBytes, 0, 8).getLong();

        Random randomWithSeed = new Random(seed);
        for (int i = 0; i < length; i++) {
            if (i < 2) {
                password.append(LOWER.charAt(randomWithSeed.nextInt(LOWER.length())));
            } else if (i == 2) {
                password.append('-');
            } else {
                password.append(DIGITS.charAt(randomWithSeed.nextInt(DIGITS.length())));
            }
        }

        return password.toString();
    }

    public static String getFakeEduPersonPrincipalName(SisuPerson person) {
        if (!DEV_MODE) {
            return null;
        }
        return DevModeUtils.pseudoRandomId(12, person
            .getFirstNames()
            + person.getLastName()
            + person.getStudentNumber()
            + person.getEmployeeNumber()) + "@helsinki.fi";
    }
}
