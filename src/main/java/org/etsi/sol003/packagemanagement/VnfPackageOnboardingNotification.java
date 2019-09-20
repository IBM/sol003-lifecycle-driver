package org.etsi.sol003.packagemanagement;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Represents a VNF package management notification, which informs the receiver of the on-boarding of a VNF package.
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Represents a VNF package management notification, which informs the receiver of the on-boarding of a VNF package.")
public class VnfPackageOnboardingNotification {

    @ApiModelProperty(name = "Identifier", required = true, notes = "Identifier of this notification. If a notification is sent multiple times due to multiple subscriptions, the \"id\" attribute of all these notifications shall have the same value.")
    private String id;
    @ApiModelProperty(name = "Notification Type", required = true, notes = "Discriminator for the different notification types. Shall be set to \"VnfPackageOnboardingNotification\" for this notification type.")
    private String notificationType;
    @ApiModelProperty(name = "Subscription Id", notes = "Identifier of the subscription that this notification relates to.")
    private String subscriptionId;
    @ApiModelProperty(name = "Notification Time", required = true, notes = "Date-time of the generation of the notification.")
    private OffsetDateTime timeStamp;
    @ApiModelProperty(name = "Onboarded VNF Package Identifier", required = true, notes = "Identifier of the on-boarded VNF package. This identifier is allocated by the NFVO.")
    private String onboardedVnfPkgId;
    @ApiModelProperty(name = "VNFD Identifier", required = true, notes = "This identifier, which is managed by the VNF provider, identifies the VNF package and the VNFD in a globally unique way. It's copied from the VNFD of the on-boarded VNF package.")
    private String vnfdId;

    @ApiModelProperty(name = "Links", required = true, notes = "Links to resources related to this notification.")
    @JsonProperty("_links")
    private PkgmLinks links;

}
