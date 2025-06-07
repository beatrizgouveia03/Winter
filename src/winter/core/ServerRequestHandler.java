package winter.core;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

public class ServerRequestHandler {
    private ServerSocket serverSocket;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port: " + port);
        
        while (true) {
            // Accept incoming connections and handle requests
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private void handleClient(Socket clientSocket) {
        try(
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
            ) {

            String requestLine = in.readLine();
            
            // Locate the request line and check if it is a POST request
            if(requestLine != null && !requestLine.startsWith("POST")) {
               sendResponse(out, 405, "Invalid request method. Only POST is allowed.");
               clientSocket.close();
               return;
            } 

            //Extract the ObjectID from the url
            String objectID = extractObjectToID(requestLine);
            if(objectID == null){
                sendResponse(out, 400, "ObjectID not found");
                clientSocket.close();
                return;
            }

            // Read headers until an empty line is encountered and locate the Content-Length header
            int contentLenght = 0;

            while (!(requestLine = in.readLine()).isEmpty()) {
                if (requestLine.startsWith("Content-Length: ")) {
                    contentLenght = Integer.parseInt(requestLine.split(":")[1].trim());
                }
                
            }

            // If Content-Length is not found or is invalid, return a 400 Bad Request response
            if (contentLenght <= 0) {
                sendResponse(out, 400, "Bad Request: Content-Length header is missing or invalid.");
                clientSocket.close();
                return;
            }
            
            // Read the request body based on the Content-Length header
            char[] bodyChars = new char[contentLenght];

            in.read(bodyChars);

            String requestBody = new String(bodyChars);

            System.out.println("Received request body: " + requestBody);

            String responseBody;

            try{
                // Process the request using the Invoker class
                responseBody = Invoker.handleRequest(objectID, requestBody);
            } catch (Exception e) {
                sendResponse(out, 500, "Internal Server Error: " + e.getMessage());
                clientSocket.close();
                return;
            }   

            // Send a 200 OK response with the processed response body
            sendResponse(out, 200, responseBody);

            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    private String extractObjectToID(String requestLine){
        int start = requestLine.indexOf("/invoke?oid=");
        
        if(start == -1) return null;

        int end = requestLine.indexOf(" ", start);

        if(end == -1) end = requestLine.length();

        return requestLine.substring(start+12, end);
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
        out.write("Content-Length: " + responseBody.length() + "\r\n");
        out.write("\r\n");
        out.write(responseBody);
        out.flush();
    }
}
