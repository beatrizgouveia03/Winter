package winter.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Invoker {
    @SuppressWarnings("unchecked")
    public static String handleRequest(String objectID, String requestBody) throws Exception {
        MethodReference methodref = ObjectIDManager.resolve(objectID);

        if(methodref == null){
            throw new RuntimeException("ObjectID not found: " + objectID);
        }

        Method method = methodref.getMethod();
        Object instance  = methodref.getInstance();
        
        Map<String, Object> request = Marshaller.fromJson(requestBody);

        List<Object> paramsList = (List<Object>) request.get("params");

        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] params =  new Object[paramTypes.length];

        for(int i=0; i<paramTypes.length; i++){
            params[i] = convertObjectToType(paramsList.get(i), paramTypes[i]);
        }

        Object response = method.invoke(instance, params);

        return Marshaller.toJson(response);
    }

    private static Object convertObjectToType(Object value, Class<?> type){
        if(value == null) return null;

        if (type == Integer.class) {
            if (value instanceof Double d) return d.intValue();
            if (value instanceof Number n) return n.intValue();
            return Integer.parseInt(value.toString());
        }

        if (type == Double.class) {
            if (value instanceof Number n) return n.doubleValue();
            return Double.parseDouble(value.toString());
        }

        if (type == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }

        if (type == String.class) {
            return value.toString();
        }

        return value;
    }
}
