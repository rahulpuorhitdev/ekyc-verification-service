package org.example.ekyc.exception;

public class HttpStatusServiceException extends ServiceException {
    private final int code;

    public int getCode() {
        return code;
    }
    public HttpStatusServiceException(String serviceName, String message, int code) {
        super(serviceName, message);
        this.code = code;
    }

}
