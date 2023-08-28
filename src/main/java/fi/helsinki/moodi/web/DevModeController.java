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

package fi.helsinki.moodi.web;

import fi.helsinki.moodi.config.DevMode;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryService;
import fi.helsinki.moodi.service.synchronize.SynchronizationService;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.util.DevModeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.exception.NotFoundException.notFoundException;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller that is enabled only in dev mode.
 * Provides UI and REST API for testing purposes.
 */
@Controller
@Conditional(DevMode.class)
public class DevModeController {

    private final Logger logger = LoggerFactory.getLogger(DevModeController.class);

    private final Environment environment;
    private final StudyRegistryService studyRegistryService;
    private final MoodleService moodleService;
    private final SynchronizationService synchronizationService;

    @Autowired
    public DevModeController(
        StudyRegistryService studyRegistryService,
        Environment environment,
        MoodleService moodleService,
        SynchronizationService synchronizationService) {

        this.studyRegistryService = studyRegistryService;
        this.environment = environment;
        this.moodleService = moodleService;
        this.synchronizationService = synchronizationService;

        logDevMode();
    }

    private void logDevMode() {
        logger.info("----------------------------------------------------------------");
        logger.info("Application is in development mode. DevModeController is active!");
        logger.info("----------------------------------------------------------------");
    }

    @RequestMapping(value = "/test", method = GET)
    public String renderIndex() {
        return "redirect:/test/course";
    }

    @RequestMapping(value = "/test/course", method = GET)
    public String renderCoursesTestPage() {
        return "course";
    }

    @RequestMapping(value = "/test/sync", method = GET)
    public String renderSyncTestPage() {
        return "sync";
    }

    @RequestMapping(value = "/test/users", method = GET)
    public String renderUsersTestPage() {
        return "users";
    }

    @RequestMapping(value = "/login", method = GET)
    public String renderLoginPage(final Model model) {
        return "login";
    }

    @RequestMapping(value = "/logout", method = POST)
    public String doLogout(final HttpServletResponse response) {
        Cookie cookie = new Cookie("client", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/login";
    }

    @RequestMapping(value = "/login", method = POST)
    public String doLogin(
            @RequestParam(value = "id", defaultValue = "") final String id,
            @RequestParam(value = "token", defaultValue = "") final String token,
            final Model model,
            final HttpServletResponse response) {

        final String clientToken = environment.getProperty("auth.client." + id);
        if (clientToken == null) {
            return loginFailed(model);
        }

        if (clientToken.equals(token)) {
            return loginSucceeded(id, token, response);
        } else {
            return loginFailed(model);
        }
    }

    @RequestMapping(value = "/api/v1/study-registry-course/{realisationId}/users", method = RequestMethod.GET)
    @ResponseBody
    public List<MoodleUser> getCourseUsers(@PathVariable("realisationId") String realisationId) {
        final StudyRegistryCourseUnitRealisation realisation = getStudyRegistryCourse(realisationId);

        final List<MoodleUser> students = realisation.students.stream()
                .map(s -> new MoodleUser("student", s.firstNames, s.lastName,
                    s.userName, s.studentNumber))
                .collect(Collectors.toList());

        final List<MoodleUser> teachers = realisation.teachers.stream()
                .map(s -> new MoodleUser("teacher", s.firstNames, s.lastName, s.userName, null))
                .collect(Collectors.toList());

        final List<MoodleUser> users = new ArrayList<>();
        users.addAll(students);
        users.addAll(teachers);

        users.forEach(u -> {
            u.moodleId = getMoodleId(u.username);
        });

        return users;
    }

    private long getMoodleId(String username) {
        try {
            return moodleService.getUser(Arrays.asList(username)).get().getId();
        } catch (Exception e) {
            return 0;
        }
    }

    @RequestMapping(value = "/api/v1/study-registry-course/{realisationId}", method = RequestMethod.GET)
    @ResponseBody
    public StudyRegistryCourseUnitRealisation getStudyRegistryCourse(@PathVariable("realisationId") String realisationId) {
        return studyRegistryService.getSisuCourseUnitRealisation(realisationId)
                .orElseThrow(notFoundException("Study registry course not found with realisation id " + realisationId));

    }

    @RequestMapping(value = "/api/v1/synchronize/polling", method = RequestMethod.GET)
    @ResponseBody
    public SynchronizationSummary synchronizePolling() {
        return synchronizationService.synchronize(SynchronizationType.INCREMENTAL);
    }

    @RequestMapping(value = "/api/v1/synchronize/full", method = RequestMethod.GET)
    @ResponseBody
    public SynchronizationSummary synchronizeFull() {
        return synchronizationService.synchronize(SynchronizationType.FULL);
    }

    @RequestMapping(value = "/api/v1/users", method = RequestMethod.POST)
    @ResponseBody
    public MoodleUser createMoodleUser(@RequestBody final CreateUserRequest request) {
        final String email = UUID.randomUUID() + ".mooditest@helsinki.fi";

        long moodleId = moodleService.createUser(request.username, request.firstName, request.lastName,
                email, DevModeUtils.generatePassword(12), request.idNumber);

        final MoodleUser moodleUser = new MoodleUser(request.role, request.firstName, request.lastName, request.username, request.idNumber);
        moodleUser.moodleId = moodleId;

        return moodleUser;
    }

    @RequestMapping(value = "/api/v1/users/{userId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Map<String, String> createMoodleUser(@PathVariable("userId") final long userId) {
        moodleService.deleteUser(userId);
        return Collections.singletonMap("result", "ok");
    }

    private String loginSucceeded(
            final String id,
            final String token,
            final HttpServletResponse response) {

        Cookie cookie = new Cookie("client", id + ":" + token);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/test";
    }

    private String loginFailed(Model model) {
        model.addAttribute("failure", true);
        return "login";
    }

    public static class MoodleUser {

        public String firstName;
        public String lastName;
        public String username;
        public String role;
        public long moodleId;
        public String idNumber;

        public MoodleUser(String role, String firstName, String lastName, String username, String idNumber) {
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.idNumber = idNumber;
        }
    }

    public static class CreateUserRequest {

        public String firstName;
        public String lastName;
        public String username;
        public String role;
        public String idNumber;
    }
}
