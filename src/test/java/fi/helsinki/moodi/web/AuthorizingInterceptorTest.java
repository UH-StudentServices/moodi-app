package fi.helsinki.moodi.web;

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