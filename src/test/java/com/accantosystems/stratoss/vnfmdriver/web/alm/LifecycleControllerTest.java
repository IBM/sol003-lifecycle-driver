package com.accantosystems.stratoss.vnfmdriver.web.alm;

import static com.accantosystems.stratoss.vnfmdriver.test.TestConstants.TEST_DL_NO_AUTH;
import static com.accantosystems.stratoss.vnfmdriver.test.TestConstants.TEST_EXCEPTION_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.etsi.sol003.common.ProblemDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.accantosystems.stratoss.vnfmdriver.driver.SOL003ResponseException;
import com.accantosystems.stratoss.vnfmdriver.model.alm.ExecutionAcceptedResponse;
import com.accantosystems.stratoss.vnfmdriver.model.alm.ExecutionRequest;
import com.accantosystems.stratoss.vnfmdriver.model.alm.FindReferenceRequest;
import com.accantosystems.stratoss.vnfmdriver.model.alm.FindReferenceResponse;
import com.accantosystems.stratoss.vnfmdriver.model.web.ErrorInfo;
import com.accantosystems.stratoss.vnfmdriver.service.LifecycleManagementService;

@AutoConfigureMockMvc(addFilters=false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test" })
public class LifecycleControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private LifecycleManagementService lifecycleManagementService;

    @Test
    public void testExecuteLifecycle() throws Exception {
        final ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setLifecycleName("Install");
        executionRequest.setDeploymentLocation(TEST_DL_NO_AUTH);

        when(lifecycleManagementService.executeLifecycle(any())).thenReturn(new ExecutionAcceptedResponse(UUID.randomUUID().toString()));

        final ResponseEntity<ExecutionAcceptedResponse> responseEntity = testRestTemplate.postForEntity("/api/driver/lifecycle/execute", executionRequest, ExecutionAcceptedResponse.class);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(responseEntity.getHeaders().getContentType()).isNotNull();
        assertThat(responseEntity.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getRequestId()).isNotEmpty();
    }

    @Test
    public void testExecuteLifecycleReturnsErrorInfo() throws Exception {
        final ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setLifecycleName("Install");
        executionRequest.setDeploymentLocation(TEST_DL_NO_AUTH);

        when(lifecycleManagementService.executeLifecycle(any()))
                .thenThrow(new SOL003ResponseException("Received SOL003-compliant error when communicating with VNFM: " + TEST_EXCEPTION_MESSAGE, new ProblemDetails(404, TEST_EXCEPTION_MESSAGE)));

        final ResponseEntity<ErrorInfo> responseEntity = testRestTemplate.postForEntity("/api/driver/lifecycle/execute", executionRequest, ErrorInfo.class);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getHeaders().getContentType()).isNotNull();
        assertThat(responseEntity.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getLocalizedMessage()).isEqualTo("Received SOL003-compliant error when communicating with VNFM: TestExceptionMessage");
        assertThat(responseEntity.getBody().getDetails().get("vnfmStatus")).isEqualTo(404);
        assertThat(responseEntity.getBody().getDetails().get("vnfmDetail")).isEqualTo(TEST_EXCEPTION_MESSAGE);
    }

    @Test
    public void testFindReferences() throws Exception {
        final FindReferenceRequest findReferenceRequest = new FindReferenceRequest();
        findReferenceRequest.setInstanceName("Instance1");

        final ResponseEntity<FindReferenceResponse> responseEntity = testRestTemplate.postForEntity("/api/driver/references/find", findReferenceRequest, FindReferenceResponse.class);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

}