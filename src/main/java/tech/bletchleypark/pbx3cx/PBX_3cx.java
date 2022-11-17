package tech.bletchleypark.pbx3cx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;
import tech.bletchleypark.beans.User;

public class PBX_3cx {

    public static void doImport() throws IOException, SQLException{
        String raw = Files.readString(Paths.get("/opt","mfiles","3CX_Extentions.json"));
        JSONArray exts= new JSONObject(raw).getJSONArray("list");
        for(int indx=0;indx<exts.length();indx++){
            JSONObject ext = exts.getJSONObject(indx);
            User user  = User.fetch(ext.optString("FirstName",""),
            ext.optString("LastName","")
            );
            if(user!=null){
                user.setExtension(ext.optString("Number", ""));
                user.setEmail(ext.optString("Email", ""));
                if(user.getOther().has("3cx")){
                    user.getOther().remove("3cx");
                }
                JSONObject pbx3cx = new JSONObject();
                pbx3cx.put("id", ext.getInt("Id"));
                user.getOther().put("3cx", pbx3cx);
                user.update();
            }
        }
        
    
    }
    
}
