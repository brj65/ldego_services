package lde.kiwi.mfiles;

import org.json.JSONObject;

public class MfilesVault {
    public final String name;
    public final String authentication;
    public final String guid;

    public MfilesVault(JSONObject jsonObject) {
        name = jsonObject.getString("Name");
        authentication = jsonObject.getString("Authentication");
        guid = jsonObject.getString("GUID");

    }

}
