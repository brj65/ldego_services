package lde.kiwi.mfiles;


import org.json.JSONObject;

public class MFile {
    public final JSONObject jsonObject;

    public MFile(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String escapedName() {
        return jsonObject.getString("EscapedName");
    }

    public int id() {
        return jsonObject.getInt("ID");
    }
}
