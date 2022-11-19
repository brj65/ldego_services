package lde.kiwi.mfiles;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.google.gson.Gson;

import static tech.bletchleypark.ApplicationLifecycle.application;;

public class MFilesAliasManger {

    public static final MFilesAliasManger instanceOf = new MFilesAliasManger();

    private List<MFilesAlias> aliasMap = new ArrayList<>();

    private MFilesAliasManger() {
    }

    public List<MFilesAlias> list() throws SQLException {
        update();
        return aliasMap;
    }
    public JSONArray fetchAllAsJsonObject() throws SQLException {
        update();
        return new JSONArray(new Gson().toJson(aliasMap));
    }

    private void update() throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rst = stmt.executeQuery("SELECT " +
                        "mfiles_alias.alias, " +
                        "mfiles_alias.display_name, " +
                        "mfiles_alias.description, " +
                        "mfiles_alias_map.vault, " +
                        "mfiles_alias_map.mf_parameter_id " +
                        "FROM " +
                        "mfiles_alias " +
                        "INNER JOIN " +
                        "mfiles_alias_map " +
                        "ON " +
                        "mfiles_alias.alias = mfiles_alias_map.alias")) {
            aliasMap.clear();
            while (rst.next()) {
                MFilesAlias alias = MFilesAlias.newInstanceOf(rst);
                aliasMap.add(alias);
            }
        }
    }

}
