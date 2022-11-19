package lde.kiwi.api.mfiles.cache_files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import lde.kiwi.mfiles.MFiles;

import static tech.bletchleypark.ApplicationLifecycle.application;
import static tech.bletchleypark.ConfigProviderManager.*;

public class CachedAsset {

    public final String vault;
    public final long mfItemId;
    public final long mfFileId;
    public final int version;
    private Path assetPath;
    public final JSONObject details;

    private final String queryUpdate = "UPDATE cached_assets set details = ?, cached_path = ?  " +
            " WHERE vault = ? AND mf_item_id = ? AND mf_file_id = ? AND version = ?";
    private final String queryInsert = "INSERT INTO cached_assets (vault, mf_item_id, mf_file_id, version, cached_path, details) VALUES (?,?,?,?,?,?)";

    public CachedAsset(ResultSet rs) throws JSONException, SQLException {
        vault = rs.getString("vault");
        mfItemId = rs.getLong("mf_item_id");
        mfFileId = rs.getLong("mf_file_id");
        details = new JSONObject(rs.getString("details"));
        assetPath = rs.getString("cached_path") == null ? generatePath() : Paths.get(rs.getString("cached_path"));
        version = rs.getInt("version");
    }

    public CachedAsset(String vault, long mfItemId, long mfFileId, int version, JSONObject details)
            throws JSONException, SQLException {
        this.vault = vault;
        this.mfItemId = mfItemId;
        this.mfFileId = mfFileId;
        this.details = details;
        this.version = version;
        generatePath();
    }

    public Path getAssetPath() {
        if (assetPath == null)
            getAssetPath();
        return assetPath;
    }

    private Path generatePath() {
        if (configHas(MFiles.vaultUpdate("mfiles.vault.path", vault))) {
            assetPath = getConfigPath(MFiles.vaultUpdate("mfiles.vault.path", vault));
        } else {
            assetPath = getConfigPath("mfiles.working.assets");
        }
        return assetPath = assetPath.resolve(vault)
                .resolve(vault + "_" + mfItemId + "_" + mfFileId + "_" + version + ".mfiles");
    }

    public CachedAsset(String vault, JSONObject asset) {
        this.vault = vault;
        this.mfItemId = asset.getLong("item_id");
        this.mfFileId = asset.getLong("id");
        this.version = asset.getInt("version");
        this.details = new JSONObject();
        generatePath();
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mf_vault", vault);
        jsonObject.put("mf_item_id", mfItemId);
        jsonObject.put("mf_file_id", mfFileId);
        jsonObject.put("mf_version", version);
        jsonObject.put("details", details);
        return jsonObject;
    }

    public void update() {

        try {
            try (Connection conn = application.defaultDataSource.getConnection();
                    PreparedStatement update = conn.prepareStatement(queryUpdate);) {
                update.setString(1, details.toString());
                update.setString(2, getAssetPath().toFile().getAbsolutePath());
                update.setString(3, vault);
                update.setLong(4, mfItemId);
                update.setLong(5, mfFileId);
                update.setLong(6, version);
                if (update.executeUpdate() == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(queryInsert);) {

                        insert.setString(1, vault);
                        insert.setLong(2, mfItemId);
                        insert.setLong(3, mfFileId);
                        insert.setLong(4, version);
                        insert.setString(5, getAssetPath().toFile().getAbsolutePath());
                        insert.setString(6, details.toString());
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<CachedAsset> fetchAll(String vault) throws SQLException {
        List<CachedAsset> assets = new ArrayList<>();
        try (Connection conn = application.defaultDataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM cached_assets WHERE vault ='" + vault + "'")) {
            while (rs.next()) {
                CachedAsset asset = new CachedAsset(rs);
                assets.add(asset);
            }
        }
        return assets;
    }

    public static CachedAsset createFromMFiles(String vault, long mfItemID, long fileId, int version,
            JSONObject mfAsset)
            throws JSONException, SQLException {
        return new CachedAsset(vault, mfItemID, fileId, version, mfAsset);
    }

    public void delete() {
    }

}
