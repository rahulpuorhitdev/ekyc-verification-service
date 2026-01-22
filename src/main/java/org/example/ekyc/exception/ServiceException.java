package org.example.ekyc.exception;

public class ServiceException extends KYCException {
    private final String serviceName;

    public ServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public ServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }
}
