package winter.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map; 
import java.lang.reflect.Type;

public class Marshaller {
    private static final Gson gson = new Gson();

    public static Map<String, Object> fromJson(String json) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public static String toJson(Object result) {
        return gson.toJson(Map.of("result", result));
    }
}
