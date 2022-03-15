package org.etsi.sol003.lifecyclemanagement;

import org.etsi.sol003.common.ResourceHandle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Represents an externally provided link port to be used to connect a VNFC connection point to an exernallymanaged VL.
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Represents an externally provided link port to be used to connect a VNFC connection point to an exernallymanaged VL.")
public class VnfLinkPortData {
    @ApiModelProperty(name = "VNF Link Port Id", required = true, notes = "Identifier of this link port as provided by the entity that has created the link port.")
    private String vnfLinkPortId;
    @ApiModelProperty(name = "Resource Handle", required = true, notes = "Reference to the virtualised network resource realizing this link port.")
    private ResourceHandle resourceHandle;


    
}
