package lde.kiwi.api.mfiles.cache_files;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import io.agroal.api.AgroalDataSource;
import static tech.bletchleypark.ApplicationLifecycle.application;

public class CachedSiteVisit {

    private final String vault;
    private final long mfilesId;
    private final JSONObject properties;
    private final JSONArray assets;
    private final JSONArray tasks;

    private String fetch = "SELECT * FROM  cached_site_visit " +
            " WHERE vault= ? AND mfiles_id = ? ";
    private String update = "UPDATE  cached_site_visit SET " +
            " properties = ? " +
            " ,assets = ? " +
            " ,tasks = ? " +
            " WHERE vault= ? AND mfiles_id = ? ";
    private String insert = "INSERT INTO cached_site_visit (properties,assets,tasks,vault,mfiles_id) "
            +
            " VALUES " +
            "(?,?,?,?,?)";

    public CachedSiteVisit(String vault, JSONObject jSiteVisit) {
        this.vault = vault;
        mfilesId = jSiteVisit.getLong("mfiles_id");
        this.properties = jSiteVisit.getJSONObject("properties");
        this.assets = jSiteVisit.getJSONArray("assets");
        this.tasks = jSiteVisit.getJSONArray("tasks");
    };

    public static CachedSiteVisit create(String vault, JSONObject jsonSiteVisit) {
        return new CachedSiteVisit(vault, jsonSiteVisit);
    }

    public CachedSiteVisit(String vault, ResultSet rs) throws SQLException {
        this.vault = vault;
        mfilesId = rs.getInt("mfiles_id");
        properties = rs.getString("properties") != null ? new JSONObject(rs.getString("properties")) : new JSONObject();
        assets = rs.getString("assets") == null ? new JSONArray() : new JSONArray(rs.getString("assets"));
        tasks = rs.getString("tasks") == null ? new JSONArray() : new JSONArray(rs.getString("tasks"));
    }

    public long getMfilesId() {
        return mfilesId;
    }

    public JSONArray getAssets() {
        return assets == null ? new JSONArray() : assets;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("vault", vault);
        json.put("mfiles_id", mfilesId);
        json.put("properties", properties);
        json.put("assets", assets);
        json.put("tasks", tasks);
        return json;
    }

    public static List<CachedSiteVisit> fetchAll(AgroalDataSource dataSource,String vault) throws SQLException {
        List<CachedSiteVisit> siteVisits = new ArrayList<>();
        try (Connection con = dataSource.getConnection();) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM cached_site_visit WHERE vault ='" + vault + "'");
            while (rs.next()) {
                CachedSiteVisit siteVisit = new CachedSiteVisit(vault, rs);
                siteVisits.add(siteVisit);
            }
            rs.close();
            stmt.close();
        }
        return siteVisits;

    }

    public static HashMap<Long, CachedSiteVisit> fetchAllAsMap(AgroalDataSource dataSource,String vault) throws SQLException {
        HashMap<Long, CachedSiteVisit> siteVisits = new HashMap<>();
        fetchAll(dataSource,vault).forEach(siteVisit -> {
            siteVisits.put(siteVisit.getMfilesId(), siteVisit);
        });
        return siteVisits;
    }

    public static JSONArray fetchAllAsJSONArray(AgroalDataSource dataSource,String vault) throws SQLException {
        JSONArray jSiteVisits = new JSONArray();
        fetchAll( dataSource,vault).forEach(siteVisit -> {
            jSiteVisits.put(siteVisit.toJSONObject());
        });
        return jSiteVisits;
    }

    public void delete() throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection(); Statement stmt = conn.createStatement();) {
            stmt.executeUpdate("DELETE FROM cached_site_visit WHERE mfiles_id = " + mfilesId);

        }
    }

    public void update() throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection();
                PreparedStatement stmtUpdate = conn.prepareStatement(update);
                PreparedStatement stmtFetch = conn.prepareStatement(fetch)) {
            stmtFetch.setString(1, vault);
            stmtFetch.setLong(2, mfilesId);
            if (stmtFetch.executeQuery().next()) {
                updatePreparedStatement(stmtUpdate);
                stmtUpdate.executeUpdate();
            } else {
                try (PreparedStatement stmtInsert = conn.prepareStatement(insert)) {
                    updatePreparedStatement(stmtInsert);
                    stmtInsert.executeUpdate();
                }
            }
        }
    }

    private void updatePreparedStatement(PreparedStatement stmt) throws SQLException {
        stmt.setString(1, properties.toString());
        stmt.setString(2, assets.toString());
        stmt.setString(3, tasks.toString());
        stmt.setString(4, vault);
        stmt.setLong(5, mfilesId);
    }

    public static boolean expired(String vault) throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT cached_tables.expires " +
                        "FROM cached_tables " +
                        "WHERE cached_tables.`table` = 'cached_site_visit' AND vault = '" + vault + "'");
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new DateTime(rs.getTimestamp(1)).isBeforeNow();
            }
            return true;
        }
    }

    public static void updateExpires(String vault) {
        try {
            try (Connection conn = application.defaultDataSource.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT cached_tables.refresh_frequency " +
                            "FROM cached_tables " +
                            "WHERE cached_tables.`table` = 'cached_site_visit' AND vault= '" + vault + "'");
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE cached_tables set expires = ?" +
                                    ",refreshing = b'0' " +
                                    "WHERE cached_tables.`table` = 'cached_site_visit' AND vault= '" + vault + "'");) {
                        update.setTimestamp(1, new Timestamp(DateTime.now().plusMinutes(rs.getInt(1)).getMillis()));
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO cached_tables (`table`,vault,refresh_frequency,expires) VALUES (?,?,?,?)");) {
                        insert.setString(1, "cached_site_visit");
                        insert.setString(2, vault);
                        insert.setInt(3, 60);
                        insert.setTimestamp(4, new Timestamp(DateTime.now().plusHours(1).getMillis()));
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setRefreshing(String vault, boolean refreshing) throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection();
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE cached_tables set expires = ?" +
                                ",refreshing = ? " +
                                "WHERE cached_tables.`table` = 'cached_site_visit' AND vault= '" + vault + "'");) {
            update.setTimestamp(1, new Timestamp(DateTime.now().getMillis()));
            update.setBoolean(2, refreshing);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO cached_tables (`table`,vault,refresh_frequency,refreshing,expires) VALUES (?,?,?,?,?)");) {
                    insert.setString(1, "cached_site_visit");
                    insert.setString(2, vault);
                    insert.setInt(3, 60);
                    insert.setBoolean(4, refreshing);
                    insert.setTimestamp(5, new Timestamp(DateTime.now().getMillis()));
                    insert.executeUpdate();
                }
            }
        }
    }

    public static boolean isRefreshing(String vault, String table) throws SQLException {
        try (Connection conn = application.defaultDataSource.getConnection();
                PreparedStatement query = conn.prepareStatement(
                        "SELECT * FROM cached_tables  " +
                                "WHERE `table` = ? AND vault= ?");) {
            query.setString(1, table);
            query.setString(2, vault);
            boolean isRefreshing = false;
            try (ResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    if (rs.getBoolean("refreshing")) {
                        if ((new DateTime(rs.getTimestamp("expires"))).plusMinutes(10).isBeforeNow()) {
                            updateExpires(vault);
                            isRefreshing = false;
                        } else {
                            isRefreshing = true;
                        }
                    }
                }
            }
            return isRefreshing;
        }
    }

}