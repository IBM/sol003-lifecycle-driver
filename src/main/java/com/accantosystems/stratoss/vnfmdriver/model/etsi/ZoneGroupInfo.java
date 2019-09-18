package com.accantosystems.stratoss.vnfmdriver.model.etsi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Represents information regarding a resource zone group. A resource zone group is a group of one or more related resource zones which can be used in resource placement constraints. To fulfil such
 * constraint, the NFVO may decide to place a resource into any zone that belongs to a particular group.
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Represents a resource zone group.")
public class ZoneGroupInfo {

    @ApiModelProperty(name = "Zone Identifier", required = true, notes = "References of identifiers of \"ZoneInfo\" structures, each of which provides information about a resource zone that belongs to this group.")
    private List<String> zoneId;

}
