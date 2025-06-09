package com.winter.core;

import java.lang.reflect.Method;

/**
 * Invoker class responsible for invoking methods on instances.
 * This class uses reflection to call methods dynamically.
 */
public class Invoker {
    
    public Object invoke(Object instance, Method method, Object[] args) throws Exception {
        // Invoke the method on the instance with the provided arguments
        return method.invoke(instance, args);
    }
}
