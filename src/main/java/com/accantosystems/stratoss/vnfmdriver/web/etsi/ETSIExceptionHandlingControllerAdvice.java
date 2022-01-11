package com.accantosystems.stratoss.vnfmdriver.web.etsi;

import javax.servlet.http.HttpServletRequest;

import org.etsi.sol003.common.ProblemDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.accantosystems.stratoss.vnfmdriver.driver.GrantProviderException;
import com.accantosystems.stratoss.vnfmdriver.driver.VNFPackageNotFoundException;
import com.accantosystems.stratoss.vnfmdriver.service.ContentRangeNotSatisfiableException;
import com.accantosystems.stratoss.vnfmdriver.service.GrantRejectedException;
import com.accantosystems.stratoss.vnfmdriver.service.PackageStateConflictException;

/**
 * Handles the conversion of Exceptions thrown by SOL003-compliant Rest API calls
 */
@RestControllerAdvice("com.accantosystems.stratoss.vnfmdriver.web.etsi")
public class ETSIExceptionHandlingControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ETSIExceptionHandlingControllerAdvice.class);

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ProblemDetails handleHttpMessageConversionException(HttpServletRequest req, HttpMessageConversionException cause) {
        return defaultHandle("Unable to parse request", cause, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ProblemDetails handleHttpMessageNotReadableException(HttpServletRequest req, HttpMessageNotReadableException cause) {
        return defaultHandle("Invalid content provided to request", cause, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ProblemDetails handleMissingServletRequestParameterException(HttpServletRequest req, MissingServletRequestParameterException cause) {
        return defaultHandle(cause.getLocalizedMessage(), cause, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ResponseBody
    protected ProblemDetails handleHttpMediaTypeNotSupportedException(HttpServletRequest req, HttpMediaTypeNotSupportedException cause) {
        return defaultHandle("Invalid content provided to request", cause, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    protected ProblemDetails handleHttpMediaTypeNotAcceptableException(HttpServletRequest req, HttpMediaTypeNotAcceptableException cause) {
        return defaultHandle("Invalid content provided to request", cause, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    protected ProblemDetails handleHttpRequestMethodNotSupportedException(HttpServletRequest req, HttpRequestMethodNotSupportedException cause) {
        return defaultHandle("Invalid method used in request", cause, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(GrantRejectedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    protected ProblemDetails handleGrantRejectedException(HttpServletRequest req, GrantRejectedException cause) {
        return defaultHandle("Grant request was rejected", cause, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NotImplementedException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ResponseBody
    protected ProblemDetails handleNotImplementedException(HttpServletRequest req, NotImplementedException cause) {
        return defaultHandle("Method not yet implemented", cause, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler(ResponseTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    protected ProblemDetails handleResponseTypeNotAcceptableException(HttpServletRequest req, ResponseTypeNotAcceptableException cause) {
        return defaultHandle("The requested response type was not acceptable", cause, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ProblemDetails handleBadRequestException(HttpServletRequest req, BadRequestException cause) {
        return defaultHandle("The request was invalid", cause, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PackageStateConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ProblemDetails handlePackageStateConflictException(HttpServletRequest req, PackageStateConflictException cause) {
        return defaultHandle("The operation cannot be executed currently, due to a conflict with the state of the resource.", cause, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ContentRangeNotSatisfiableException.class)
    @ResponseStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
    @ResponseBody
    protected ProblemDetails handleContentRangeNotSatisfiableException(HttpServletRequest req, ContentRangeNotSatisfiableException cause) {
        return defaultHandle("The byte range passed in the \"Range\" header did not match any available byte range in the VNF Package file.", cause, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @ExceptionHandler(VNFPackageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ProblemDetails handleVNFPackageNotFoundException(HttpServletRequest req, VNFPackageNotFoundException cause) {
        return defaultHandle("The VNF Package could not be found.", cause, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GrantProviderException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected ProblemDetails handleGrantProviderException(HttpServletRequest req, GrantProviderException cause) {
        return defaultHandle("Grant request could not be handled.", cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected ProblemDetails handleAllExceptions(HttpServletRequest req, Exception cause) {
        logger.error("Attempting to handle an exception of type [{}]", cause.getClass().getSimpleName());
        return defaultHandle(cause.getLocalizedMessage(), cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ProblemDetails defaultHandle(String message, Exception cause, HttpStatus statusCode) {
        if (logger.isDebugEnabled()) {
            logger.warn(message, cause);
        } else {
            logger.warn(String.format("%s: %s", message, cause.getMessage()));
        }
        return new ProblemDetails(statusCode.value(), cause.getLocalizedMessage());
    }

}
