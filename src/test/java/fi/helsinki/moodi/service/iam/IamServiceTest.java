package fi.helsinki.moodi.service.iam;


import fi.helsinki.moodi.integration.iam.IAMClient;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static fi.helsinki.moodi.service.iam.IAMService.TEACHER_ID_PREFIX;
import static org.mockito.Mockito.*;

public class IamServiceTest extends AbstractMoodiIntegrationTest {

    private static final String TEACHER_ID = "0123456";
    private static final String PREFIXED_TEACHER_ID = TEACHER_ID_PREFIX + TEACHER_ID;

    private IAMService iamService;

    private IAMClient mockIamClient;

    @Before
    public void setup() {
        mockIamClient = mock(IAMClient.class);
        iamService = new IAMService(mockIamClient);
    }

    @Test
    public void thatTeacherIdPrefexIsAddedWhenMissing() {
        iamService.getTeacherUsernameList(TEACHER_ID);

        verify(mockIamClient, times(1)).getTeacherUsernameList(PREFIXED_TEACHER_ID);
    }

    @Test
    public void thatTeacherIdPrefixIsNotAddedIfAlreadyExists() {
        iamService.getTeacherUsernameList(PREFIXED_TEACHER_ID);

        verify(mockIamClient, times(1)).getTeacherUsernameList(PREFIXED_TEACHER_ID);
    }
}
