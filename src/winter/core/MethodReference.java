package winter.core;

import java.lang.reflect.Method;

public class MethodReference {
    private final Method method;
    private final Object instance;

    public MethodReference(Object instance, Method method){
        this.method = method;
        this.instance = instance;
    }

    public Method getMethod(){ return method; }
    public Object getInstance(){ return instance; }
}
