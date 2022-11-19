package lde.kiwi.mfiles;

import org.json.JSONObject;

public class MFilesVault {
    public final String name;
    public final String authentication;
    public final String guid;

    public MFilesVault(JSONObject jsonObject) {
        name = jsonObject.getString("Name");
        authentication = jsonObject.getString("Authentication");
        guid = jsonObject.getString("GUID");

    }
}
