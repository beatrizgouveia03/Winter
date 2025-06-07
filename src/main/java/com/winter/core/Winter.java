package com.winter.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.winter.annotations.*;

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

public class Winter {
    private Invoker invoker;
    private Marshaller marshaller;

    private Map<String, Map<MethodHTTP, RemoteMethodEntry>> remoteMethodsRegistry;

    public Winter(Invoker invoker, Marshaller marshaller) {
        this.invoker = invoker;
        this.marshaller = marshaller;
        this.remoteMethodsRegistry = new HashMap<>();
    }

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

    public String handleRequest(String methodHTTP, String uri, String requestBody, Map<String,String> headers) throws Exception {
        //Retrieve the method entry based on the HTTP method
        MethodHTTP requestMethod = MethodHTTP.valueOf(methodHTTP.toUpperCase());
        Map<String, String> queryParams = new HashMap<>();
        String originalUri = uri;

        // Check if the URI contains a query string
        int queryStartIndex = uri.indexOf('?');
        if (queryStartIndex != -1) {
            originalUri = uri.substring(0, queryStartIndex); // Update URI to remove query string
            // Extract query string
            String queryString = uri.substring(queryStartIndex + 1);
            queryParams = parseQueryString(queryString);
        }

        
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

        return response;
    }

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
