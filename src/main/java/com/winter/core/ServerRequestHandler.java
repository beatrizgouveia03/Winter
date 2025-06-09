package com.winter.core;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import java.net.ServerSocket;

/**
 * ServerRequestHandler is responsible for handling incoming HTTP requests
 * and sending appropriate responses back to the client.
 * It listens for incoming connections on a specified port and processes
 * requests using the Winter framework's Broker class.
 */
public class ServerRequestHandler {

    private Winter winter;
    private Marshaller marshaller;
    private int port = 8080; // Default port
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public ServerRequestHandler(Winter winter, Marshaller marshaller) {
        this.winter = winter;
        this.marshaller = marshaller;
    }

    /**
     * Starts the server and listens for incoming requests on the specified port.
     * If the port is already in use, it will throw an IOException.
     * @throws IOException
     */
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

    /**
     * Stops the server and closes the server socket.
     */
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

    /**
     * Handles incoming client requests.
     * Reads the request line, headers, and body, processes the request,
     * and sends an appropriate response back to the client.
     * @param clientSocket The socket connected to the client.
     */
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

            String uri = requestParts[1]; //Ex: /randomGenerator/generateUsername?username=test
            String method = requestParts[0]; //Ex: GET, POST, etc.



            // Read headers until an empty line is encountered and locate the Content-Length header
            String headerLine;
            int contentLength = 0;
            Map<String, String> headers = new HashMap<>();

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
                        
                        }
                    }
                }
                
            }
        
            // If the method is POST or PUT and has content length read the request body
            String requestBody = "";
            if(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
                if(contentLength > 0){
                    // Read the request body based on the Content-Length header
                    int result;
                    int bytesRead = 0;
                    char[] bodyChars = new char[contentLength];

                    while (bytesRead < contentLength && (result = in.read(bodyChars, bytesRead, contentLength - bytesRead)) != -1) {
                        bytesRead += result;

                    }

                    requestBody = new String(bodyChars, 0, bytesRead);

                    System.out.println("Received request body: " + requestBody);
                }
            }

            try{
                // Process the request using the Broker class
                Response response = winter.handleRequest(method, uri, requestBody, headers);
               
                sendResponse(out, response.getStatusCode(), response.getBody());
            } catch (Exception e) {

                System.err.println("Unexpected Error: " + e.getMessage());
                e.printStackTrace();

                String errorBody;

                try{
                    errorBody = marshaller.marshal(new RemoteError(500, " Server Error: " + e.getMessage(), e.getClass().getSimpleName()));
                } catch (Exception marshalException) {
                    System.err.println("Error marshalling error response: " + marshalException.getMessage());

                    errorBody = "{\"code\":500, \"message\": \"Failed to marshal error response, \"type\": \"Internal Server Error\"}";

                    sendResponse(out, 500, errorBody);
                } finally{
                    try{
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                    } catch (IOException ex) {
                        System.err.println("Error closing client socket: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    /**
     * Sends an HTTP response back to the client.
     * @param out The BufferedWriter to send the response.
     * @param statusCode The HTTP status code to send.
     * @param responseBody The body of the response.
     * @throws IOException If an error occurs while sending the response.
     */
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
