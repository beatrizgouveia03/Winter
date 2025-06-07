package winter.core;

import java.util.Map;

import winter.annotations.RemoteMethod;
import winter.annotations.RemoteComponent;

import java.util.HashMap;
import java.lang.reflect.Method;

public class ComponentRegistry {
    private static final Map<String, Object> remoteObjects = new HashMap<>();
    private static final Map<String, Method> remoteMethods = new HashMap<>();

    public static void registerComponent(Object instance) {
        Class<?> clazz = instance.getClass();

        if (clazz.isAnnotationPresent(RemoteComponent.class)) {
            RemoteComponent annotation = clazz.getAnnotation(RemoteComponent.class);
            String componentName = annotation.name().isEmpty() ? clazz.getSimpleName() : annotation.name();

            remoteObjects.put(componentName, instance);

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RemoteMethod.class)) {    
                    String methodName = method.getName();
                    String key = buildMethodKey(componentName, methodName, method.getParameterTypes());

                    remoteMethods.put(key, method);
                }
            }
        }
    }

    public static Object getComponent(String name) {
        return remoteObjects.get(name);
    }

    public static Method getMethod(String component, String method, Class<?>[] paramTypes) {
        String key = buildMethodKey(component, method, paramTypes);
        
        return remoteMethods.get(key);
    }

    private static String buildMethodKey(String component, String method, Class<?>[] paramTypes){
        StringBuilder key = new StringBuilder(component + "/" + method);
        for(Class<?> param : paramTypes) key.append(param.getSimpleName());

        return key.toString();
    }
}
