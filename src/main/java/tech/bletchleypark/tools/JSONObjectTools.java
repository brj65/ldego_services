package tech.bletchleypark.tools;

import org.json.JSONObject;

public class JSONObjectTools {

    public static JSONObject  JSONObjectNew(String key,Object object){
        JSONObject jobject = new JSONObject();
        jobject.put(key, object);
        return jobject;
    }
    
}
