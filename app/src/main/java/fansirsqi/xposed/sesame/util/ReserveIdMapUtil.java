package fansirsqi.xposed.sesame.util;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReserveIdMapUtil {

    private static final Map<String, String> idMap = new ConcurrentHashMap<>();

    private static final Map<String, String> readOnlyIdMap = Collections.unmodifiableMap(idMap);

    public static Map<String, String> getMap() {
        return readOnlyIdMap;
    }

    public static String get(String key) {
        return idMap.get(key);
    }

    public static synchronized void add(String key, String value) {
        idMap.put(key, value);
    }

    public static synchronized void remove(String key) {
        idMap.remove(key);
    }

    public static synchronized void load() {
        idMap.clear();
        try {
            String body = Files.readFromFile(Files.getReserveIdMapFile());
            if (!body.isEmpty()) {
                Map<String, String> newMap = JsonUtil.parseObject(body, new TypeReference<Map<String, String>>() {
                });
                idMap.putAll(newMap);
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public static synchronized boolean save() {
        return Files.write2File(JsonUtil.toJsonString(idMap), Files.getReserveIdMapFile());
    }

    public static synchronized void clear() {
        idMap.clear();
    }

}
