package com.accantosystems.stratoss.vnfmdriver.web.etsi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.etsi.sol003.packagemanagement.VnfPkgInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.accantosystems.stratoss.vnfmdriver.config.VNFMDriverProperties;
import com.accantosystems.stratoss.vnfmdriver.driver.VNFPackageNotFoundException;
import com.accantosystems.stratoss.vnfmdriver.service.ContentRangeNotSatisfiableException;
import com.accantosystems.stratoss.vnfmdriver.service.PackageManagementService;
import com.accantosystems.stratoss.vnfmdriver.service.PackageStateConflictException;
import com.accantosystems.stratoss.vnfmdriver.service.UnexpectedPackageContentsException;

import io.swagger.annotations.ApiOperation;

@RestController("PackageManagementController")
@RequestMapping("/vnfpkgm/v1/vnf_packages")
@ConditionalOnProperty(value = "vnfmdriver.packageManagement.enabled", matchIfMissing  = false)
public class PackageManagementController {

    private final static Logger logger = LoggerFactory.getLogger(PackageManagementController.class);

    private static final String CONTENT_TYPE_APPLICATION_YAML = "application/yaml";
    private static final String CONTENT_TYPE_APPLICATION_ZIP = "application/zip";

    private final PackageManagementService packageManagementService;
    private final VNFMDriverProperties vnfmDriverProperties;

    @Autowired
    public PackageManagementController(PackageManagementService packageManagementService, VNFMDriverProperties vnfmDriverProperties) {
        this.packageManagementService = packageManagementService;
        this.vnfmDriverProperties = vnfmDriverProperties;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Query VNF packages information.", notes = "Queries the information of the VNF packages matching the filter.")
    public ResponseEntity<List<VnfPkgInfo>> queryVnfPackages(@RequestParam(value = "filter", required = false) String filter,
                                                             @RequestParam(value = "all_fields", required = false) String allFields,
                                                             @RequestParam(value = "fields", required = false) String fields,
                                                             @RequestParam(value = "exclude_fields", required = false) String excludeFields,
                                                             @RequestParam(value = "exclude_default", required = false) String excludeDefault,
                                                             @RequestParam(value = "nextpage_opaque_marker", required = false) String nextPageOpaqueMarker,
                                                             @RequestParam(value = "vnfdId", required = false) String vnfdId) throws VNFPackageNotFoundException {
        logger.info("Received VNF Package Query.");

        // NFV-3251 - Special case to support Mavenir integration. We assume the VNFD Id will be the same as the VNF Pkg Id
        if (StringUtils.hasText(vnfdId)) {
            return ResponseEntity.ok(Collections.singletonList(packageManagementService.getVnfPackageInfo(vnfdId)));
        }

        return ResponseEntity.ok(packageManagementService.getAllVnfPackageInfos(vnfmDriverProperties.getPackageManagement().getNexusGroupName()));
    }

    @GetMapping(path = "/{vnfPkgId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Reads the information of an individual VNF package", notes = "This resource represents an individual VNF package. The client can use this resource to read information of the VNF package.")
    public ResponseEntity<VnfPkgInfo> getVnfPackage(@PathVariable String vnfPkgId) throws VNFPackageNotFoundException {
        logger.info("Received Individual VNF package Info Get request.");

        VnfPkgInfo vnfInfo = packageManagementService.getVnfPackageInfo(vnfPkgId);
        return ResponseEntity.ok(vnfInfo);

    }

    @GetMapping(path = "/{vnfPkgId}/vnfd", produces = { MediaType.TEXT_PLAIN_VALUE, CONTENT_TYPE_APPLICATION_ZIP })
    @ApiOperation(value = "Reads the content of the VNFD within a VNF package.", notes = "This resource represents the VNFD contained in an on-boarded VNF package. The client can use this resource to obtain the content of the VNFD.")
    @ExceptionHandler(ResponseTypeNotAcceptableException.class)
    public ResponseEntity<?> getVnfd(@RequestHeader("Accept") List<String> acceptHeader, @PathVariable String vnfPkgId) throws VNFPackageNotFoundException {

        logger.info("Received VNFD Get request for package id [{}]", vnfPkgId);

        boolean acceptsZip;
        if (acceptHeader.isEmpty()) {
            throw new ResponseTypeNotAcceptableException("No response type specified in Accept HTTP header. ");
        } else {
            List<String> acceptTypes = new ArrayList<>(acceptHeader);
            // remove all acceptable types from the list
            acceptsZip = acceptTypes.remove(CONTENT_TYPE_APPLICATION_ZIP);
            acceptTypes.remove(MediaType.TEXT_PLAIN_VALUE); // text/plain is an allowed content type
            acceptTypes.remove(MediaType.APPLICATION_JSON_VALUE); // need to accept application/json in case a ProblemDetails response is returned
            // anything left is unacceptable
            if (!acceptTypes.isEmpty()) {
                throw new ResponseTypeNotAcceptableException(String.format("Response type(s) not acceptable in Accept HTTP header: [%s]", String.join(",", acceptTypes)));
            }
        }

        if (acceptsZip) {
            // application/zip is accepted (even if text/plain is also accepted) so return all as zip
            Resource zipResource = packageManagementService.getVnfdAsZip(vnfPkgId);
            HttpHeaders headers = new HttpHeaders();
            // TODO set content length: headers.setContentLength(?);
            headers.setContentType(MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_ZIP));
            return new ResponseEntity<>(zipResource, headers, HttpStatus.OK);
        } else {
            // text/plain is the only accepted type. Return as YAML only
            try {
                String vnfd = packageManagementService.getVnfdAsYaml(vnfPkgId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<>(vnfd, headers, HttpStatus.OK);
            } catch (UnexpectedPackageContentsException e) {
                throw new ResponseTypeNotAcceptableException(String.format("The contents of the VNF Package were unexpected for the given Accept HTTP header: [%s]", String.join(",", acceptHeader)),
                                                             e);
            }
        }

    }

    @GetMapping(path = "/{vnfPkgId}/package_content", produces = { CONTENT_TYPE_APPLICATION_ZIP })
    @ApiOperation(value = "Reads the content of a VNF package identified by the VNF package identifier allocated by the NFVO.", notes = "This resource represents a VNF package identified by the VNF package identifier allocated by the NFVO. The client can use this resource to fetch the content of the VNF package.")
    public ResponseEntity<Resource> getVnfPackageContent(@RequestHeader(value = "Content-Range", required = false) String contentRange,
                                                         @PathVariable String vnfPkgId) throws PackageStateConflictException,
                                                                                               ContentRangeNotSatisfiableException, VNFPackageNotFoundException {

        logger.info("Received VNF Package Content Get request for package id [{}] and content range [{}]", vnfPkgId, contentRange);

        Resource vnfPackage = packageManagementService.getVnfPackageContent(vnfPkgId, contentRange);
        HttpStatus responseStatus = contentRange != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_ZIP));
        return new ResponseEntity<>(vnfPackage, headers, responseStatus);

    }

    // The actual mapping we want is "/{vnfPkgId}/artifacts/{artifactPath}" but it's not possible to match on this when the artifact path contains "/" characters (even when encoded)
    // This path filter seems to be the only way to match on these URLs - https://stackoverflow.com/questions/51108291
    @GetMapping(path = { "/{vnfPkgId}/artifacts/**" })
    @ApiOperation(value = "Reads the content content of an artifact within a VNF package.", notes = "This resource represents an individual artifact contained in a VNF package. The client can use this resource to fetch the content of the artifact.")
    public ResponseEntity<Resource> getVnfPackageArtifact(@RequestHeader(value = "Content-Range", required = false) String contentRange, @PathVariable String vnfPkgId,
                                                          HttpServletRequest request) throws PackageStateConflictException, ContentRangeNotSatisfiableException, VNFPackageNotFoundException {

        // Need to manually extract the artifactPath from the request URI to ensure it supports slashes within it
        String requestPath = request.getRequestURI();
        int index = requestPath.indexOf("/artifacts/") + "/artifacts/".length();
        String artifactPath = request.getRequestURI().substring(index);

        logger.info("Received VNF Package Artifact Get request for package id [{}], artifact path [{}] and content range [{}]", vnfPkgId, artifactPath, contentRange);

        Resource vnfPackageArtifact = packageManagementService.getVnfPackageArtifact(vnfPkgId, artifactPath, contentRange);
        HttpStatus responseStatus = contentRange != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // TODO - can we determine the actual type from the content
        return new ResponseEntity<>(vnfPackageArtifact, headers, responseStatus);

    }

}
