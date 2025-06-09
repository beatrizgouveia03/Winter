package com.winter.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.winter.annotations.*;

/**
 * Represents a remote method entry in the registry.
 * Contains the instance of the remote object, the method itself, and the HTTP method annotation.
 */
class RemoteMethodEntry{
    private Object instance; // Instance of the remote object 
    private Method method; // The remote method itself
    private MethodHTTP methodHTTP; // HTTP method annotation

    public RemoteMethodEntry(Object instance, Method method, MethodHTTP methodHTTP) {
        this.instance = instance;
        this.method = method;
        this.methodHTTP = methodHTTP;
    }

    public Object getInstance() {
        return instance;
    }

    public Method getMethod() {
        return method;
    }

    public MethodHTTP getMethodHTTP() {
        return methodHTTP;
    }
}

/**
 * The Winter class is the core of the Winter framework.
 * It handles the registration of remote objects and their methods,
 * and processes incoming HTTP requests by routing them to the appropriate remote method.
 */
public class Winter {
    private Invoker invoker;
    private Marshaller marshaller;

    private Map<String, Map<MethodHTTP, RemoteMethodEntry>> remoteMethodsRegistry;

    public Winter(Invoker invoker, Marshaller marshaller) {
        this.invoker = invoker;
        this.marshaller = marshaller;
        this.remoteMethodsRegistry = new HashMap<>();
    }

    /**
     * Registers a remote object instance and its methods for remote invocation.
     * The class must be annotated with @RemoteObject and each method must be annotated with @RemoteMethod.
     * @param remoteObjInstance
     */
    public void registerRemoteMethods(Object remoteObjInstance) {
        Class<?> clazz = remoteObjInstance.getClass();
       
        if(!clazz.isAnnotationPresent(RemoteObject.class)) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Remote");
        }

        RemoteObject remoteObjectAnnotation = clazz.getAnnotation(RemoteObject.class);
        String classPath = remoteObjectAnnotation.value();

        for(Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(RemoteMethod.class)) {
                RemoteMethod remoteMethodAnnotation = method.getAnnotation(RemoteMethod.class);
                String methodPath = remoteMethodAnnotation.path();
                MethodHTTP methodHTTP = remoteMethodAnnotation.method();

                String fullURI = (classPath.isEmpty() ? "" : classPath + "/") + methodPath;

                remoteMethodsRegistry
                    .computeIfAbsent(fullURI, _ -> new HashMap<>())
                    .put(methodHTTP, new RemoteMethodEntry(remoteObjInstance, method, methodHTTP));

                System.out.println("Registered remote method: " + fullURI + " with HTTP method: " + methodHTTP);
            }
        }
    }


    /**
     * Handles an incoming HTTP request by routing it to the appropriate remote method.
     * @param methodHTTP The HTTP method of the request (GET, POST, etc.)
     * @param uri The URI of the request
     * @param requestBody The body of the request (if applicable)
     * @param headers The headers of the request
     * @return A response string, which may be a result or an error message in JSON format
     * @throws Exception If an error occurs during processing
     */
    public Response handleRequest(String methodHTTP, String uri, String requestBody, Map<String,String> headers) throws Exception {
        //Retrieve the method entry based on the HTTP method
        MethodHTTP requestMethod = null;

        try {
            requestMethod = MethodHTTP.valueOf(methodHTTP.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException("Unsupported HTTP method: " + methodHTTP);
        }

        String originalUri = uri;
        Map<String, String> queryParams = new HashMap<>();

        // Check if the URI contains a query string
        int queryStartIndex = uri.indexOf('?');
        if (queryStartIndex != -1) {
            originalUri = uri.substring(0, queryStartIndex); // Update URI to remove query string
            // Extract query string
            String queryString = uri.substring(queryStartIndex + 1);
            queryParams = parseQueryString(queryString);
        }

        try{
            // Check if the URI is registered and retrieve the corresponding method entries
            Map<MethodHTTP, RemoteMethodEntry> methodMap = remoteMethodsRegistry.get(originalUri);
            
            if (methodMap == null) {
                throw new IllegalArgumentException("No remote method found for URI: " + uri);
            }

            RemoteMethodEntry entry = methodMap.get(requestMethod);

            if (entry == null) {
                throw new UnsupportedOperationException("HTTP method " + requestMethod + " not supported for URI: " + uri);
            }

            //Unmarshal the request body to the appropriate parameters
            Object[] args = marshaller.unmarshalParameters(entry.getMethod(), requestBody, queryParams);

            //Invoke the method using the invoker
            Object result = invoker.invoke(entry.getInstance(), entry.getMethod(), args);

            //Marshal the result to a response string
            String response = marshaller.marshal(result);

            return new Response(response, 200);
        } catch (IllegalArgumentException e) {
            // Illegal Argument Exception (400 Bad Request)
            System.err.println("Request Error (400 - Bad Request): " + e.getMessage());
            String errorJson = marshaller.marshal(new RemoteError(400, "Bad Request: " + e.getMessage(), e.getClass().getSimpleName()));
            return new Response(errorJson, 400);
        } catch (UnsupportedOperationException e) {
            //Method Not Allowed Exception (405 Method Not Allowed)
            System.err.println("Method Error (405 - Method Not Allowed): " + e.getMessage());
            String errorJson =  marshaller.marshal(new RemoteError(405, "Method Not Allowed: " + e.getMessage(), e.getClass().getSimpleName()));
            return new Response(errorJson,405 );
        } catch (Exception e) {
            // Any other exception (500 Internal Server Error)
            System.err.println("Internal Server Error (500 - Internal Server Error): " + e.getMessage());
            e.printStackTrace(); // Imprimir stack trace no servidor para depuração
            String errorJson =  marshaller.marshal(new RemoteError(500, "Internal Server Error: " + e.getMessage(), e.getClass().getSimpleName()));
            return new Response(errorJson, 500);
        }
    }

    /**
     * Parses a query string into a map of parameters.
     * @param queryString
     * @return A map of query parameters where the key is the parameter name and the value is the parameter value.
     */
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        // If the query string is null or empty, return an empty map
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }

        Arrays.stream(queryString.split("&"))
            .forEach(param -> {
                int equalIndex = param.indexOf("=");
                if (equalIndex != -1) {
                    String name = param.substring(0, equalIndex);
                    String value = param.substring(equalIndex + 1);
                    params.put(name, value);
                } 
            });

        return params;
    }
    
}
