package winter.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectIDManager {
    private static final Map<String, String> IDsGenerated = new HashMap<>();
    private static final Map<String, MethodReference> IDToMethod = new HashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(1);

    public static String generateObjectID(String componentName, String methodName){
        //Format: component-method-00
        String objectID = componentName + "-" + methodName +  "-" + counter.getAndIncrement();
        IDsGenerated.put(componentName+"-"+methodName, objectID);

        return objectID;
    }

    public static void register(String objectID, MethodReference methodRef){
        IDToMethod.put(objectID,  methodRef);
    }

    public static MethodReference resolve(String objectID){
        return IDToMethod.get(objectID);
    }

    public static String lookup(String componentName){
        return IDsGenerated.get(componentName);
    }
    
}
