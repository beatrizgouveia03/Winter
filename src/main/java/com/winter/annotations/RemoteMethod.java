package com.winter.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RemoteMethod {
    String path() default ""; // The path to the remote method, used for routing requests
    MethodHTTP method() default MethodHTTP.GET; // The HTTP method to use (GET, POST, etc.)
    
}


