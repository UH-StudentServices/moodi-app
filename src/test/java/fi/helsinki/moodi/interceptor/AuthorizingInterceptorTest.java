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

package fi.helsinki.moodi.interceptor;

import fi.helsinki.moodi.exception.UnauthorizedException;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertTrue;

public class AuthorizingInterceptorTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private AuthorizingInterceptor authorizingInterceptor;

    @Test(expected = UnauthorizedException.class)
    public void requestIsUnauthorizedWithoutClientHeaders() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        authorizingInterceptor.preHandle(request, null, null);
    }

    @Test(expected = UnauthorizedException.class)
    public void requestIsUnauthorizedInvalidClientHeaders() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("client-id", "invalid");
        request.addHeader("client-token", "not valid");
        authorizingInterceptor.preHandle(request, null, null);
    }

    @Test(expected = UnauthorizedException.class)
    public void requestIsUnauthorizedIfClientIdIsInvalid() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("client-id", "invalid");
        request.addHeader("client-token", "xxx123");
        authorizingInterceptor.preHandle(request, null, null);
    }

    @Test(expected = UnauthorizedException.class)
    public void requestIsUnauthorizedIfClientTokenIsInvalid() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("client-id", "testclient");
        request.addHeader("client-token", "humbdidumbdi");
        authorizingInterceptor.preHandle(request, null, null);
    }

    @Test
    public void requestIsAuthorizedWithCorrectClientHeaders() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("client-id", "testclient");
        request.addHeader("client-token", "xxx123");
        assertTrue(authorizingInterceptor.preHandle(request, null, null));
    }
}