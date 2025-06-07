package com.winter.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.winter.annotations.Param;

public class Marshaller {
    private Gson gson;

    public Marshaller() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Object[] unmarshalParameters(Method method, String requestBody, Map<String, String> queryParams) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        Map<String, Object> jsonBodyMap = new HashMap<>();

        if(requestBody != null && !requestBody.isEmpty()) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            jsonBodyMap = gson.fromJson(requestBody, type);
        }

        for(int i=0; i<parameters.length; i++){
            Parameter parameter = parameters[i];
            String paramName = null;
            
            if(parameter.isAnnotationPresent(Param.class)) {
                Param paramAnnotation = parameter.getAnnotation(Param.class);
                paramName = paramAnnotation.name();
            } else {
                paramName = parameter.getName();
            }

            if (paramName == null || paramName.isEmpty()) {
                throw new IllegalArgumentException("Parameter " + parameter.getName() + " in method " + method.getName() + " is missing @Param annotation.");
            }

            String stringValue = null;
            Object rawValue = null;
            
            //Try to get the value from the query string (GET request)
            if(queryParams.containsKey(paramName)) {
                stringValue = queryParams.get(paramName);
            } else if(jsonBodyMap.containsKey(paramName)) {
                // If not found in query params, try to get it from the JSON body(POST/PUT request)
               rawValue = jsonBodyMap.get(paramName);
                if(rawValue != null) {
                    if(rawValue instanceof Number){
                        if (parameter.getType() == int.class || parameter.getType() == Integer.class) {
                            stringValue = String.valueOf(((Number) rawValue).intValue());
                        } else if (parameter.getType() == long.class || parameter.getType() == Long.class) {
                            stringValue = String.valueOf(((Number) rawValue).longValue());
                        } else if (parameter.getType() == double.class || parameter.getType() == Double.class) {
                            stringValue = String.valueOf(((Number) rawValue).doubleValue());
                        } else if (parameter.getType() == float.class || parameter.getType() == Float.class) {
                            stringValue = String.valueOf(((Number) rawValue).floatValue());
                        } else {
                            stringValue = rawValue.toString(); // Catch-all
                        }
                    } else {
                        stringValue = rawValue.toString();
                    }
                }
            }

            Object convertedValue = convertStringtoType(stringValue, parameter.getType());
            args[i] = convertedValue;
        }

        return args;
    }

    public String marshal(Object object) throws Exception {
        if (object == null) {
            return "{}"; // Return an empty JSON object for null values
        }

        //Return the JSON representation of the object
        return gson.toJson(object);
    }

    private Object convertStringtoType(String value, Class<?> type) throws IllegalArgumentException {
        if (value == null || value.isEmpty()) {
            return null; // Return null for empty values
        }

        // Handle primitive types and their wrappers
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == String.class) {
            return value;
        }

        throw new IllegalArgumentException("Unsupported parameter type for direct conversion: " + type.getName() + " for value: " + value);
    }
}
