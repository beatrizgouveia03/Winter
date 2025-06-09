package com.winter.core;

/**
 * Represents an error response from a remote service.
 * Contains the error code, message, and type.
 */
public class RemoteError {
    private int code;
    private String message;
    private String type; 

    public RemoteError(int code, String message, String type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}
