package com.feritoth.cla.springmvc.controller.exception;

import org.springframework.http.HttpStatus;

/**
 * The class that supplies the error templates for the user-friendly error detection. 
 * 
 * @author Frantisek Slovak
 */
public class ExceptionInfo {
	
	// List of attributes: the URL of the REST method which throws an error and the message of the exception itself.
	private final String url;
    private final String exceptionMessage;
    private final int errorCode;
    private final HttpStatus httpOperationStatus;

    /**
     * First parameterized class constructor - used for user-friendly exception creation on the server side.
     * 
     * @param url the URL of the REST method where the exception was thrown
     * @param detectedException the detected exception which was thrown by the invoked REST method
     */
    public ExceptionInfo(String url, Exception detectedException, int detectedErrorCode, HttpStatus operationStatus) {
        this.url = url;
        this.exceptionMessage = detectedException.getMessage();
        this.errorCode = detectedErrorCode;
        this.httpOperationStatus = operationStatus;
    }   

    /**
     * Second parameterized class constructor - used for exception handling on the client side.
     * 
     * @param url the URL of the REST method where the exception was thrown
     * @param exceptionMessage the message of the detected exception which was thrown by the invoked REST method
     */
    public ExceptionInfo(String url, String exceptionMessage, int detectedErrorCode, HttpStatus operationStatus) {
		super();
		this.url = url;
		this.exceptionMessage = exceptionMessage;
		this.errorCode = detectedErrorCode;
		this.httpOperationStatus = operationStatus;
	}
    
	/**
     * @return the URL of the invoked REST method
     */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @return the error code carried by the identified exception
	 */
	public int getErrorCode() {
		return errorCode;
	}
	
	/**
	 * @return the final operation status
	 */
	public HttpStatus getHttpOperationStatus() {
		return httpOperationStatus;
	}

	/**
	 * @return the message carried by the detected exception
	 */
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	@Override
	public String toString() {
		return "ExceptionInfo [url=" + url + ", errorCode=" + errorCode + ", httpStatus=" + httpOperationStatus.name() + ", exceptionMessage=" + exceptionMessage + "]";
	}	
	
}