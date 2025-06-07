package com.winter.core;

import java.lang.reflect.Method;

public class Invoker {
    
    public Object invoke(Object instance, Method method, Object[] args) throws Exception {
        // Invoke the method on the instance with the provided arguments
        return method.invoke(instance, args);
    }
}
