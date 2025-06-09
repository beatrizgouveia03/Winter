package com.winter.core;

/**
 * Represents a response from a remote service.
 * Contains the body of the response, the HTTP status code.
 */
public class Response {
    private String body;
    private int statusCode;
    
    public Response(String body, int statusCode) {
        this.body = body;
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }
    public int getStatusCode() {
        return statusCode;
    }
}
