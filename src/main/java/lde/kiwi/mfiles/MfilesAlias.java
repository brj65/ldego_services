package lde.kiwi.mfiles;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MFilesAlias {
    public final int id;
    public final int mfParameterId;
    public final String vault;
    public final String description;
    public final String displayName;

    public MFilesAlias(ResultSet rst) throws SQLException {
        id = rst.getInt("alias");
        displayName = rst.getString("display_name");
        description = rst.getString("description");
        vault = rst.getString("vault");
        mfParameterId = rst.getInt("mf_parameter_id");

    }

    public static MFilesAlias newInstanceOf(ResultSet rst) throws SQLException {
        return new MFilesAlias(rst);
    }

}
