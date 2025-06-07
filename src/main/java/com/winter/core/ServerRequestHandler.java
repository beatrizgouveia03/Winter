package com.winter.core;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import java.net.ServerSocket;

public class ServerRequestHandler {

    private int port = 8080; // Default port
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Winter winter;

    public ServerRequestHandler(Winter winter) {
        this.winter = winter;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        if (serverSocket == null) {
            throw new IOException("Failed to create server socket on port: " + port);
        }

        running = true;
        System.out.println("Server started on port: " + port);
        
        while (running) {
            try{
                // Accept incoming connections and handle requests
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                } else {
                    System.out.println("Server stopped.");
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try(
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
            ) {

            String requestLine = in.readLine();
            
            // Locate the request line and check if it is null or empty
            // If the request line is null or empty, return a 400 Bad Request response
            if (requestLine == null || requestLine.isEmpty()) {
                sendResponse(out, 400, "Bad Request: Empty request line.");
                return;
            }

            // Log the request line for debugging
            System.out.println("Received request: " + requestLine);

            //Check the request line for  method, URI and HTTP version
            String[] requestParts = requestLine.split(" ");

            if (requestParts.length < 3) {
                sendResponse(out, 400, "Bad Request: Invalid request line.");
                clientSocket.close();
                return;
            }

            String method = requestParts[0]; //Ex: GET, POST, etc.
            String uri = requestParts[1]; //Ex: /randomGenerator/generateUsername?username=test



            // Read headers until an empty line is encountered and locate the Content-Length header
            Map<String, String> headers = new HashMap<>();
            int contentLength = 0;
            String headerLine;

            while ((headerLine = in.readLine()) != null && !(headerLine = in.readLine()).isEmpty()) {
                int colonIndex = headerLine.indexOf(":");
                if (colonIndex != -1) {
                    String headerName = headerLine.substring(0, colonIndex).trim();
                    String headerValue = headerLine.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                    if (headerName.equalsIgnoreCase("Content-Length")) {
                        try {
                            contentLength = Integer.parseInt(headerValue);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid Content-Length header: " + headerValue);
                            contentLength = -1; // Set to -1 to indicate an invalid length
                        }
                    }
                }
                
            }

            // If Content-Length is not found or is invalid, return a 400 Bad Request response
            if (contentLength <= 0) {
                sendResponse(out, 400, "Bad Request: Content-Length header is missing or invalid.");
                clientSocket.close();
                return;
            }
        
            // If the method is POST or PUT and has content length read the request body
            String requestBody = "";
            if(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
                if(contentLength > 0){
                    // Read the request body based on the Content-Length header
                    char[] bodyChars = new char[contentLength];
                    int bytesRead = 0;
                    int result;

                    while (bytesRead < contentLength && (result = in.read(bodyChars, bytesRead, contentLength - bytesRead)) != -1) {
                        bytesRead += result;
                    }

                    requestBody = new String(bodyChars, 0, bytesRead);

                    System.out.println("Received request body: " + requestBody);
                }
            }


            String responseBody = "";
            int statusCode = 200; // Default to 200 OK

            try{
                // Process the request using the Broker class
                responseBody = winter.handleRequest(method, uri, requestBody, headers);
            } catch (IllegalArgumentException e) {
                statusCode = 400; // Bad Request or invalid parameters
                responseBody = "{ \"error\": \"" + e.getMessage() + "\" }";
                System.err.println("Bad Request Error (400): " + e.getMessage());
            } catch (UnsupportedOperationException e) {
                statusCode = 405; // Method Not Allowed
                responseBody = "{ \"error\": \"" + e.getMessage() + "\" }";
                System.err.println("Method Not Allowed Error (405): " + e.getMessage());
            } catch (Exception e) {
                statusCode = 500; // Internal Server Error
                responseBody = "{ \"error\": \"Internal Server Error: " + e.getMessage() + "\" }";
                System.err.println("Internal Server Error: (500): " + e.getMessage());
                //e.printStackTrace();
            }

            // Send a 200 OK response with the processed response body
            sendResponse(out, statusCode, responseBody);

            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
            //e.printStackTrace();
        } finally{
            try{
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void sendResponse(BufferedWriter out, int statusCode, String responseBody) throws IOException {
        String statusMessage = switch(statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown Status";
        };

        // Write the HTTP response headers
        out.write("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("Content-Length: " + responseBody.getBytes().length + "\r\n");
        out.write("\r\n");
        out.write(responseBody);
        out.flush();
    }
}
