package com.accantosystems.stratoss.vnfmdriver.web.etsi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.accantosystems.stratoss.common.utils.LoggingUtils;
import com.accantosystems.stratoss.vnfmdriver.model.MessageDirection;
import com.accantosystems.stratoss.vnfmdriver.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.accantosystems.stratoss.vnfmdriver.model.alm.ExecutionAsyncResponse;
import com.accantosystems.stratoss.vnfmdriver.model.alm.ExecutionStatus;
import com.accantosystems.stratoss.vnfmdriver.model.alm.FailureDetails;
import org.etsi.sol003.lifecyclemanagement.LcmOperationStateType;
import org.etsi.sol003.lifecyclemanagement.LifecycleManagementNotification;
import org.etsi.sol003.lifecyclemanagement.VnfLcmOperationOccurenceNotification;
import com.accantosystems.stratoss.vnfmdriver.service.ExternalMessagingService;

import io.swagger.annotations.ApiOperation;

@RestController("LifecycleNotificationController")
@RequestMapping("/vnflcm/v2/notifications")
public class LifecycleNotificationController {

    private final static Logger logger = LoggerFactory.getLogger(LifecycleNotificationController.class);

    private final ExternalMessagingService externalMessagingService;

    @Autowired
    public LifecycleNotificationController(ExternalMessagingService externalMessagingService) {
        this.externalMessagingService = externalMessagingService;
    }

    @PostMapping
    @ApiOperation(value = "Receives a lifecycle operation occurrence notification from a VNFM", code = 204)
    public ResponseEntity<Void> receiveNotification(@RequestBody LifecycleManagementNotification notification) {
        // TODO This should be reduced to DEBUG level, but it assists in development testing to see all notification messages being received
        logger.info("Received notification:\n{}", notification);
        UUID uuid = UUID.randomUUID();

        if (notification instanceof VnfLcmOperationOccurenceNotification) {
            final VnfLcmOperationOccurenceNotification vnfLcmOpOccNotification = (VnfLcmOperationOccurenceNotification) notification;
            LoggingUtils.logEnabledMDC(notification.toString(), MessageType.REQUEST, MessageDirection.RECEIVED, uuid.toString(), MediaType.APPLICATION_JSON.toString(), "https",null ,vnfLcmOpOccNotification.getVnfLcmOpOccId());
            // Send an update if this is completed
            if (vnfLcmOpOccNotification.getNotificationStatus() == VnfLcmOperationOccurenceNotification.NotificationStatus.RESULT){
                ExecutionAsyncResponse asyncResponse = new ExecutionAsyncResponse(vnfLcmOpOccNotification.getVnfLcmOpOccId(), ExecutionStatus.COMPLETE, null, Collections.emptyMap(), Collections.emptyMap());
                // If the operation state is anything other than COMPLETED, than assume we've failed (could be FAILED, FAILED_TEMP or ROLLED_BACK)
                if (vnfLcmOpOccNotification.getOperationState() != LcmOperationStateType.COMPLETED) {
                    asyncResponse.setStatus(ExecutionStatus.FAILED);
                    // Set the failure details if we have an error message
                    if (vnfLcmOpOccNotification.getError() != null && !StringUtils.isEmpty(vnfLcmOpOccNotification.getError().getDetail())) {
                        asyncResponse.setFailureDetails(new FailureDetails(FailureDetails.FailureCode.INTERNAL_ERROR, vnfLcmOpOccNotification.getError().getDetail()));
                    }
                }
                externalMessagingService.sendExecutionAsyncResponse(asyncResponse);
            }
            LoggingUtils.logEnabledMDC("", MessageType.RESPONSE, MessageDirection.SENT,uuid.toString(),MediaType.APPLICATION_JSON.toString(), "https",null,vnfLcmOpOccNotification.getVnfLcmOpOccId());
        }

        return ResponseEntity.noContent().build();
    }

}
