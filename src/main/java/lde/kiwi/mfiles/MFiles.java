package lde.kiwi.mfiles;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lde.kiwi.api.mfiles.cache_files.CachedAsset;
import lde.kiwi.api.mfiles.cache_files.CachedSiteVisit;
import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.HttpPostMultipart;
import tech.bletchleypark.tools.StringTools;

import org.eclipse.microprofile.config.ConfigProvider;
import org.joda.time.DateTime;

import static tech.bletchleypark.ConfigProviderManager.*;

public class MFiles {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public HashMap<String, MFilesVault> vaults = new HashMap<>();
    private String mfAuthenticationToken;
    private String serverURL;
    private String superLogin;
    private String superPassword;
    private final HashMap<String, MFilesVaultAlias> cardsAlias = new HashMap<>();
    {
        cardsAlias.put("project_test_site_visits", new MFilesVaultAlias("project_test", "Site Visits", 103));
        cardsAlias.put("project_site_visits", new MFilesVaultAlias("project", "Site Visits", 103));
    }

    public static MFiles mFiles() {
        return new MFiles();
    }

    public String getSuperLogin() {
        return superLogin;
    }

    public void setSuperLogin(String superLogin) {
        this.superLogin = superLogin;
    }

    public String getSuperPassword() {
        return superPassword;
    }

    public void setSuperPassword(String superPassword) {
        this.superPassword = superPassword;
    }

    private String vault;
    private HashMap<Integer, String> prop = new HashMap<>();
    private String cacheSiteVisitId = "cached_site_visit";

    public MFiles() {
        serverURL = ConfigProvider.getConfig().getValue("mfiles.url", String.class);
        superLogin = ConfigProvider.getConfig().getValue("mfiles.user.super.login", String.class);
        superPassword = ConfigProvider.getConfig().getValue("mfiles.user.super.password", String.class);
    }

    public MFiles(String serverURL) throws MalformedURLException {
        this.serverURL = serverURL;
    }

    public void refresh() throws IOException {
        fetchVaults(mfAuthenticationToken);
    }

    public String login(String username, String password) throws IOException {
        JSONObject auth = new JSONObject();
        auth.put("Username", username);
        auth.put("Password", password);
        mfAuthenticationToken = fetch("POST", "/server/authenticationtokens", null, auth);
        return mfAuthenticationToken;
    }

    public String login(String username, String password, String vault) throws IOException {
        this.vault = vault;
        JSONObject auth = new JSONObject();
        auth.put("Username", username);
        auth.put("Password", password);
        auth.put("VaultGuid", this.vault);
        mfAuthenticationToken = fetch("POST", "/server/authenticationtokens", null, auth);
        return mfAuthenticationToken;
    }

    public String getProperties(String token) throws IOException {
        JSONArray properties = new JSONArray(fetch("GET", "/structure/properties", token, null));
        for (int idx = 0; idx < properties.length(); idx++) {
            JSONObject property = properties.getJSONObject(idx);
            prop.put(property.getInt("ID"), property.getString("Name"));

        }
        return "";
    }

    public String fetchObjectAll(String token) throws IOException {
        JSONObject json = new JSONObject(fetch("GET", "/objects/103?o=103", token, null));
        JSONArray array = json.getJSONArray("Items");
        for (int idx = 0; idx < array.length(); idx++) {
            System.out.println("MFILES?????" + array.getJSONObject(idx).getString("DisplayID"));
        }
        return json.toString(5);

    }

    public String fetchObject(String token) throws IOException {
        if (prop.isEmpty()) {
            getProperties(token);
        }
        JSONObject json = new JSONObject(fetch("GET", "/objects/103/6565/latest?include=properties", token, null));
        JSONArray properties = json.getJSONArray("Properties");
        JSONArray object = new JSONArray();
        for (int idx = 0; idx < properties.length(); idx++) {
            JSONObject property = new JSONObject();
            property.put("PropertyDef", properties.getJSONObject(idx).getInt("PropertyDef"));
            property.put("PropertyName", prop.get(properties.getJSONObject(idx).getInt("PropertyDef")));
            if (properties.getJSONObject(idx).has("PropertyDef")) {
                property.put("Value", properties.getJSONObject(idx).getJSONObject("Value").get("Value"));
            }
            property.put("DataType", properties.getJSONObject(idx).getJSONObject("Value").get("DataType"));

            object.put(property);
        }

        return object.toString();
    }

    public void rebuildAllAssets() {
        buildProperties();

    }

    public void buildProperties() {

    }

    public String getMfAuthenticationToken() {
        return mfAuthenticationToken;
    }

    public String fetch(String method, String path, String xAuthentication, JSONObject params) throws IOException {
        URL url = new URL(serverURL + "/REST" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        String postData = params != null ? params.toString(5) : "";
        if (xAuthentication != null)
            conn.setRequestProperty("X-Authentication", xAuthentication);
        conn.setRequestProperty("Content-Type", "application/application/json");
        if (params != null)
            conn.setRequestProperty("Content-Length", "" + postData.getBytes().length);
        // conn.setRequestProperty("Content-Language", "en-US");
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        if (params != null)
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData.getBytes());
            }
        BufferedReader buffer = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        String data = "";
        while ((inputLine = buffer.readLine()) != null) {
            data += inputLine;
        }
        buffer.close();
        conn.disconnect();
        return data;
    }

    public String fetchProperties(String vaultAuthenticationToken, String vault) throws IOException {
        String auth = vault != null ? vaults.get(vault).authentication : vaultAuthenticationToken;
        return fetch("GET", "/structure/properties", auth, null);
    }

    public MFilesVault fetchVault(String name) {
        return vaults.get(name);
    }

    public String fetchVaults(String mfAuthenticationToken) throws IOException {
        JSONArray json = new JSONArray(fetch("GET", "/server/vaults", mfAuthenticationToken, null));
        vaults.clear();
        for (int idx = 0; idx < json.length(); idx++) {
            MFilesVault vault = new MFilesVault(json.getJSONObject(idx));
            vaults.put(vault.name, vault);
        }
        return json.toString();
    }

    public String fetchObjectTypes(String mfAuthenticationToken2) throws IOException {
        return fetch("GET", "structure/objecttypes", mfAuthenticationToken, null);
    }

    public void touch(int type, long objectId, int propertyDef, String sql) {

    }

    public JSONObject uploadFile(Path fileToUpload) throws IOException {
        mfAuthenticationToken = new JSONObject(login(
                getConfigString(vaultUpdate("mfiles.vault.username", vault)),
                getConfigString(vaultUpdate("mfiles.vault.password", vault)),
                getConfigString(vaultUpdate("mfiles.vault.vaultGuid", vault))))
                .getString("Value");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36");
        headers.put("X-Authentication",
                mfAuthenticationToken);
        headers.put("Content-Length", "" + Files.size(fileToUpload));
        HttpPostMultipart multipart = new HttpPostMultipart(serverURL + "/REST/files", "utf-8",
                headers);
        multipart.addFilePart("bob", fileToUpload.toAbsolutePath().toFile());
        return new JSONObject(multipart.finish());
    }

    public Path downloadFile(String vault, int o, long oDocument, long oFileId)
            throws JSONException, IOException {
        return null;
    }

    public Path downloadFile(CachedAsset asset)
            throws JSONException, IOException {
        String xAuthentication = xAuthentication(asset.vault);
        URL url = new URL(
                serverURL + "/REST" + "/objects/" + 0 + "/" + asset.mfItemId + "/files/" + asset.mfFileId + "/content");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (xAuthentication != null)
            conn.setRequestProperty("X-Authentication", xAuthentication);
        conn.setRequestProperty("Content-Type", "application/application/json");
        // if (params != null)
        // conn.setRequestProperty("Content-Length", "" + postData.getBytes().length);
        // conn.setRequestProperty("Content-Language", "en-US");
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        int contentLength = conn.getContentLength();
        InputStream is = conn.getInputStream();
        if (!Files.exists(asset.getAssetPath().getParent())) {
            Files.createDirectories(asset.getAssetPath().getParent());
        }
        FileOutputStream fos = new FileOutputStream(asset.getAssetPath().toFile());
        int read = 0;
        int downloaded = 0;
        byte[] buffer = new byte[32768];
        while ((read = is.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            downloaded += read;
            System.out.println("MFiles download status of " + asset.getAssetPath().getFileName().toString() + ": "
                    + (downloaded * 100) / (contentLength * 1.0) + "%");
        }
        fos.close();
        is.close();

        conn.disconnect();
        return asset.getAssetPath();
    }

    public static String vaultUpdate(String path, String vault) {
        if (vault == null || vault.isBlank())
            return path;
        vault = vault.toLowerCase().replaceAll(" ", "_");
        return path.substring(0, path.indexOf("vault."))
                + "vault."
                + vault
                + path.substring(path.indexOf("vault.") + 5);
    }

    public List<MFile> fetchFiles(String vault, int oFileId) throws JSONException, IOException {
        return fetchFiles(vault, 0, oFileId, null);
    }

    public List<MFile> fetchFiles(String vault, int oType, int oID, Integer oVersion)
            throws JSONException, IOException {
        List<MFile> files = new ArrayList<>();
        String json = fetch(HttpMethod.GET, vault,
                serverURL + "/REST" + "/objects/" + oType + "/" + oID + (oVersion == null ? "" : "/" + oVersion)
                        + "/files",
                null);
        JSONArray array = new JSONArray(json);
        for (int idx = 0; idx < array.length(); idx++) {
            files.add(new MFile(array.getJSONObject(idx)));
        }
        return files;
    }

    public JSONObject fetchPropertyDefinitions(String vault) throws JSONException, IOException {
        JSONObject propertyDefinitions = new JSONObject();
        JSONArray mfiles = new JSONArray(fetch(HttpMethod.GET, vault,
                serverURL + "/REST" + "/structure/properties", null));
        for (int idx = 0; idx < mfiles.length(); idx++) {
            JSONObject propertyDefinition = new JSONObject();
            propertyDefinition.put("id", mfiles.getJSONObject(idx).get("ID"));
            propertyDefinition.put("name", mfiles.getJSONObject(idx).get("Name"));
            propertyDefinition.put("data_type", mfiles.getJSONObject(idx).get("DataType"));
            propertyDefinitions.put(String.valueOf(propertyDefinition.get("id")), propertyDefinition);
        }
        return propertyDefinitions;
    }

    public synchronized JSONObject fetchSiteVisits(String vault, long mfClassId, int mfQueryParam, String alias,
            String params,
            boolean allProperties, boolean refresh, int limit)
            throws JSONException, IOException, SQLException {
        if (alias != null) {
            mfClassId = cardsAlias.get(vault.toLowerCase() + "_" + alias.toLowerCase().replace(" ", "_")).mfilesId;
        }
        if (CachedSiteVisit.isRefreshing(vault, cacheSiteVisitId))
            return loadSiteVisits(vault, allProperties, limit);
        return CachedSiteVisit.expired(vault) || refresh
                ? updateObjectsCache(vault, mfClassId, mfQueryParam, params, allProperties, limit)
                : loadSiteVisits(vault, allProperties, limit);
    }

    public synchronized JSONObject updateObjectsCache(String vault, long mfClassId, int mfParameterId,
            String fetchParameters,
            boolean allProperties,
            int limit)
            throws JSONException, IOException {
        JSONObject mfilePropertiesDefinitions = fetchPropertyDefinitions(vault);
        JSONArray objects = new JSONArray();
        try {
            int start = getConfigInt(vaultUpdate("mfiles.vault.site_visit.start", vault));
            int end = getConfigInt(vaultUpdate("mfiles.vault.site_visit.end", vault));
            DateTime startDateTime = DateTime.now().minusDays(start);
            DateTime endDateTime = DateTime.now().plusDays(end);
            if (fetchParameters == null || fetchParameters.isEmpty())
                fetchParameters = "p" + mfParameterId + ">>=" + startDateTime.toString("yyyy-MM-dd'T'KK:mm:ss'Z'")
                        + "&p" + mfParameterId + "<<="
                        + endDateTime.toString("yyyy-MM-dd'T'KK:mm:ss'Z'");
            // Update DB to indicate refreshing has started
            CachedSiteVisit.setRefreshing(vault, true);
            // Get Site Visits from mFiles
            JSONObject results = new JSONObject(fetch(HttpMethod.GET, vault,
                    serverURL + "/REST" + "/objects?limit=60000&o=" + mfClassId + "&"
                            + (fetchParameters != null ? fetchParameters : ""),
                    null));
            JSONArray mfObject = results.optJSONArray("Items");
            limit = limit > 0 && limit < mfObject.length() ? limit : mfObject.length();
            // Build Site Visits

            for (int mfObjectIndex = 0; mfObjectIndex < limit; mfObjectIndex++) {
                logger.info("Processing Site Visit " + (mfObjectIndex + 1) + " of " + mfObject.length());
                long mfObjectId = mfObject.getJSONObject(mfObjectIndex).getJSONObject("ObjVer").optLong("ID");

                // Get Value and Definition for each property
                JSONArray mfObjectProperties = new JSONArray(fetch(HttpMethod.GET, vault,
                        serverURL + "/REST" + "/objects/" + mfClassId + "/" + mfObjectId + "/properties", null));
                JSONObject siteVisitProperties = new JSONObject();
                for (int mfObjectPropertiesIndex = 0; mfObjectPropertiesIndex < mfObjectProperties
                        .length(); mfObjectPropertiesIndex++) {
                    JSONObject mfilesProperty = mfObjectProperties.getJSONObject(mfObjectPropertiesIndex);

                    JSONObject objectProperty = new JSONObject();
                    int def = mfilesProperty.getInt("PropertyDef");
                    if (mfilePropertiesDefinitions.has("" + def)) {
                        objectProperty.put("property_def", mfilePropertiesDefinitions.getJSONObject("" + def));
                    }
                    if (mfilesProperty.getJSONObject("Value").has("Value"))
                        objectProperty.put("value", mfilesProperty.getJSONObject("Value").get("Value"));
                    else if (mfilesProperty.getJSONObject("Value").has("Lookups")) {
                        JSONArray mfLookups = mfilesProperty.getJSONObject("Value").getJSONArray("Lookups");
                        JSONArray lookupsItems = new JSONArray();
                        for (int mfLookupsIndex = 0; mfLookupsIndex < mfLookups.length(); mfLookupsIndex++) {
                            lookupsItems.put(mfLookups.getJSONObject(mfLookupsIndex).getInt("Item"));
                        }
                        objectProperty.put("lookups_items", lookupsItems);
                    } else
                        objectProperty.put("value", "-");

                    siteVisitProperties.put(objectProperty.getJSONObject("property_def").getInt("id") + "",
                            objectProperty);
                }
                // Build Json object
                JSONObject siteVisit = new JSONObject();
                siteVisit.put("vault", vault);
                siteVisit.put("mfiles_id", mfObjectId);
                siteVisit.put("properties", siteVisitProperties);
                siteVisit.put("assets", fetchSiteVisitAssets(vault, mfClassId, mfObjectId, siteVisitProperties));
                JSONArray siteVisitTasks = new JSONArray();
                JSONObject siteVisitTask = new JSONObject();
                siteVisitTask.put("status", "new");
                siteVisitTask.put("order", 1);
                siteVisitTask.put("title", "Site Visit Hazard Form");
                siteVisitTask.put("short_description", "Standard Site Visit Hazard Form. Required for each visit.");
                siteVisitTask.put("required", true);
                siteVisitTask.put("when", "ARRIVE_ONSITE");
                siteVisitTask.put("type", "FORM");
                JSONObject formTask = new JSONObject();
                formTask.put("form", "siteinspectionreport");
                formTask.put("type", "formio");
                siteVisitTask.put("details", formTask);
                siteVisitTasks.put(siteVisitTask);
                siteVisit.put("tasks", siteVisitTasks);
                objects.put(siteVisit);

            }
            new Thread("updateObjectsCache") {
                public void run() {
                    try {
                        try (Connection conn = ApplicationLifecycle.application.defaultDataSource.getConnection();
                                Statement stmt = conn.createStatement()) {
                            try {
                                HashMap<Long, CachedSiteVisit> siteVisitsMap = new HashMap<>();
                                for (int idx = 0; idx < objects.length(); idx++) {
                                    CachedSiteVisit siteVisit = CachedSiteVisit.create(vault,
                                            objects.getJSONObject(idx));
                                    siteVisitsMap.put(siteVisit.getMfilesId(), siteVisit);
                                }
                                HashMap<Long, CachedSiteVisit> siteVisitsDatabase = CachedSiteVisit
                                        .fetchAllAsMap(ApplicationLifecycle.application.defaultDataSource, vault);
                                List<Long> deleteThese = new ArrayList<>(siteVisitsDatabase.keySet());
                                deleteThese.removeAll(siteVisitsMap.keySet());
                                deleteThese.forEach(id -> {
                                    try {
                                        siteVisitsDatabase.get(id).delete();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                });
                                siteVisitsMap.values().forEach(siteVisit -> {
                                    try {
                                        siteVisit.update();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        CachedSiteVisit.updateExpires(vault);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }.start();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        JSONObject responseSiteVisit = new JSONObject();
        responseSiteVisit.put("site_visits", objects);
        return updateResponseSiteVisit(vault, responseSiteVisit);
    }

    private synchronized JSONObject loadSiteVisits(String vault, boolean allProperties, int limit)
            throws JSONException, IOException {
        JSONArray siteVisits = new JSONArray();
        try {
            siteVisits = CachedSiteVisit.fetchAllAsJSONArray(ApplicationLifecycle.application.defaultDataSource, vault);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        JSONObject response = new JSONObject();
        response.put("site_visits", siteVisits);
        return updateResponseSiteVisit(vault, response);
    }

    public JSONArray fetchSiteVisitAssets(String vault, long mfClassID, long mfObjectId, JSONObject objectProperties)
            throws JSONException, IOException {
        JSONObject mfilePropertiesDefinitions = fetchPropertyDefinitions(vault);
        JSONArray siteVisitAssets = new JSONArray();
        for (String propertyId : getConfigList(vaultUpdate("mfiles.vault.site_visit.assets", vault))) {
            if (!StringTools.isNumeric(propertyId)) {
                propertyId = "" + mfilePropertiesDefinitions.optInt(propertyId, -1);
            }
            if (objectProperties.has(propertyId)) {
                try {
                    if (objectProperties.getJSONObject(propertyId).has("lookups_items")) {
                        JSONArray items = objectProperties.getJSONObject(propertyId).getJSONArray("lookups_items");
                        for (int itemsIndex = 0; itemsIndex < items.length(); itemsIndex++) {
                            JSONArray mfAssets = new JSONArray(fetch(HttpMethod.GET, vault,
                                    serverURL + "/REST" + "/objects/0/" + items.get(itemsIndex) + "/files", null));
                            for (int assetIndex = 0; assetIndex < mfAssets.length(); assetIndex++) {
                                JSONObject asset = new JSONObject();
                                asset.put("item_id", items.get(itemsIndex));
                                asset.put("id", mfAssets.getJSONObject(assetIndex).getInt("ID"));
                                asset.put("version", mfAssets.getJSONObject(assetIndex).getInt("Version"));
                                asset.put("name", mfAssets.getJSONObject(assetIndex).getString("Name"));
                                asset.put("extension", mfAssets.getJSONObject(assetIndex).getString("Extension"));
                                siteVisitAssets.put(asset);
                                CachedAsset.createFromMFiles(vault, items.getLong(itemsIndex),
                                        mfAssets.getJSONObject(assetIndex).getInt("ID"),
                                        mfAssets.getJSONObject(assetIndex).getInt("Version"),
                                        mfAssets.getJSONObject(assetIndex)).update();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
        // Get Related documents
        JSONArray mfRelatedObjects = new JSONArray(fetch(HttpMethod.GET, vault,
                serverURL + "/REST" + "/objects/" + mfClassID + "/" + mfObjectId
                        + "/relationships?direction=both&type=objectversion",
                null));
        for (int mfRelatedObjectIndex = 0; mfRelatedObjectIndex < mfRelatedObjects.length(); mfRelatedObjectIndex++) {
            JSONObject mfRelatedObject = mfRelatedObjects.getJSONObject(mfRelatedObjectIndex);
            JSONArray mfRelatedObjectFiles = mfRelatedObjects.getJSONObject(mfRelatedObjectIndex).getJSONArray("Files");
            for (int mfRelatedObjectFilesIndex = 0; mfRelatedObjectFilesIndex < mfRelatedObjectFiles
                    .length(); mfRelatedObjectFilesIndex++) {
                JSONObject mfFile = mfRelatedObjectFiles.getJSONObject(mfRelatedObjectFilesIndex);
                JSONObject asset = new JSONObject();
                asset.put("item_id", mfRelatedObject.getJSONObject("ObjVer").getLong("ID"));
                asset.put("id", mfFile.getInt("ID"));
                asset.put("version", mfFile.getInt("Version"));
                asset.put("name", mfFile.getString("Name"));
                asset.put("extension", mfFile.getString("Extension"));
                siteVisitAssets.put(asset);
                try {
                    CachedAsset.createFromMFiles(vault, asset.getLong("item_id"),
                            asset.getInt("id"),
                            asset.getInt("version"),
                            mfFile).update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }

        return siteVisitAssets;
    }

    private JSONObject updateResponseSiteVisit(String vault, JSONObject response) {
        try {
            try (Connection conn = ApplicationLifecycle.application.defaultDataSource.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("SELECT * FROM cached_tables WHERE vault = ? && `table` = ?");) {
                ps.setString(1, vault);
                ps.setString(2, "cached_site_visit");
                try (ResultSet rs = ps.executeQuery();) {
                    if (rs.next()) {
                        response.put("expires", new DateTime(rs.getTimestamp("expires")));
                        response.put("refreshing", rs.getBoolean("refreshing"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public JSONObject fetchCard(String vault, long cardId, String alias) {
        JSONObject card = new JSONObject();
        try {
            if (alias != null) {
                cardId = cardsAlias.get(vault.toLowerCase() + "_" + alias.toLowerCase().replace(" ", "_")).mfilesId;
            }
            JSONObject defs = fetchPropertyDefinitions(vault);
            String json = fetch(HttpMethod.GET, vault,
                    serverURL + "/REST" + "/structure/classes?objtype=" + cardId, null);
            card = new JSONArray(json).getJSONObject(0);
            JSONArray properties = card.getJSONArray("AssociatedPropertyDefs");
            for (int idx = 0; idx < properties.length(); idx++) {
                JSONObject obj = properties.getJSONObject(idx);
                obj.put("def", new JSONObject(defs.optString(obj.getInt("PropertyDef") + "")));
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return card;
    }

    private enum HttpMethod {
        GET, POST, PUT, DELETE
    }

    public Path fetchFile(String vault, int objectID, int fileId) throws JSONException, IOException {
        Path path = fetchFile(HttpMethod.GET, vault,
                serverURL + "/REST" + "/objects/0/" + objectID + "/files/" + fileId + "/content",
                null);
        return path;
    }

    private Path fetchFile(HttpMethod method, String vault, String url, JSONObject postData)
            throws JSONException, IOException {
        Path path = Files.createTempFile("mfile", ".tmp");
        String xAuthentication = xAuthentication(vault);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method.toString());
        conn.setRequestProperty("Content-Type", "application/application/json");
        if (xAuthentication != null)
            conn.setRequestProperty("X-Authentication", xAuthentication);

        if (postData != null)
            conn.setRequestProperty("Content-Length", "" + postData.toString().getBytes().length);

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(postData == null ? false : true);
        if (postData != null)
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData.toString().getBytes());
            }
        // Getting content Length
        int contentLength = conn.getContentLength();
        System.out.println("File contentLength = " + contentLength + " bytes");

        // Requesting input data from server
        InputStream inputStream = conn.getInputStream();

        // Open local file writer
        FileOutputStream outputStream = new FileOutputStream(path.toFile());

        // Limiting byte written to file per loop
        byte[] buffer = new byte[2048];

        // Increments file size
        int length;
        int downloaded = 0;

        // Looping until server finishes
        while ((length = inputStream.read(buffer)) != -1) {
            // Writing data
            outputStream.write(buffer, 0, length);
            downloaded += length;
            System.out.println("Uploading Status: " + (downloaded * 100) / (contentLength * 1.0) + "%");

        }
        conn.disconnect();
        return path;
    }

    private String fetch(HttpMethod method, String vault, String url, JSONObject postData)
            throws JSONException, IOException {
        String xAuthentication = xAuthentication(vault);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method.toString());
        conn.setRequestProperty("Content-Type", "application/application/json");
        if (xAuthentication != null)
            conn.setRequestProperty("X-Authentication", xAuthentication);

        if (postData != null)
            conn.setRequestProperty("Content-Length", "" + postData.toString().getBytes().length);

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(postData == null ? false : true);
        if (postData != null)
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData.toString().getBytes());
            }
        BufferedReader buffer = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        String data = "";
        while ((inputLine = buffer.readLine()) != null) {
            data += inputLine;
        }
        buffer.close();
        conn.disconnect();
        return data;
    }

    public String xAuthentication(String vault) throws JSONException, IOException {
        try {
            String result = login(
                    getConfigString(vaultUpdate("mfiles.vault.username", vault)),
                    getConfigString(vaultUpdate("mfiles.vault.password", vault)),
                    getConfigString(vaultUpdate("mfiles.vault.vaultGuid", vault)));
            try {
                return new JSONObject(result)
                        .getString("Value");
            } catch (JSONException e) {
                throw new IOException("Error while login into mfiles - issue was " + result);
            }
        } catch (java.util.NoSuchElementException ex) {
            throw new IOException("Error while login into mfiles - issue was with config " + ex.getMessage());
        }
    }

    public JSONObject checkIn(JSONArray uploadAssets, String vault) throws JSONException, IOException {
        // JSONObject json = uploadAssets.getJSONObject(0);
        for (int idx = 0; idx < uploadAssets.length(); idx++) {
            String prams;
            if (uploadAssets.getJSONObject(idx).getString("Extension").equalsIgnoreCase("jpg")) {
                prams = paramsImage.replace("Description", "test");
            } else {
                prams = params2.replace("Description", "test");
            }
            JSONObject pj = new JSONObject(prams);
            JSONArray jf = pj.getJSONArray("Files");
            jf.put(uploadAssets.getJSONObject(idx));
            // TODO: handle results
            fetch(HttpMethod.POST, vault,
                    serverURL + "/REST/objects/0?checkIn=true", pj);
        }
        // TODO: handle results
        return null;

    }

    public void cleanCacheFilesForVaults(String vaults, Consumer<String> logger) throws SQLException {
        for (String vault : vaults.split(",")) {
            cleanCacheFiles(vault, logger);
        }
    }

    public JSONArray fetchAssets(String vault) throws SQLException {
        JSONArray assets = new JSONArray();
        CachedAsset.fetchAll(vault).forEach(asset -> {
            assets.put(asset.toJSONObject());
        });

        return assets;
    }

    public JSONArray cleanCacheFiles(String vault, Consumer<String> logger) throws SQLException {
        JSONArray files = new JSONArray();
        List<CachedAsset> siteVisitAssets = new ArrayList<>();
        CachedSiteVisit.fetchAll(ApplicationLifecycle.application.defaultDataSource, vault).forEach(visit -> {
            JSONArray tmpAssets = visit.getAssets();
            for (int tmpAssetIndex = 0; tmpAssetIndex < tmpAssets.length(); tmpAssetIndex++) {
                JSONObject asset = tmpAssets.getJSONObject(tmpAssetIndex);
                siteVisitAssets.add(new CachedAsset(vault, asset));
            }
        });
        List<CachedAsset> cachedAssetsDelete = new ArrayList<>();
        cachedAssetsDelete.forEach(asset -> {
            asset.delete();
        });
        siteVisitAssets.forEach(asset -> {

            // Check for latest version

            // Check asset is downloaded and readyState
            if (!Files.exists(asset.getAssetPath())) {
                logger.accept("Download for " + asset.getAssetPath().getFileName().toString() + " started ");
                try {
                    downloadFile(asset);
                    logger.accept("Download for " + asset.getAssetPath().getFileName().toString() + " completed ");
                } catch (JSONException | IOException e) {
                    logger.accept("Download for " + asset.getAssetPath().getFileName().toString() + " failed ("
                            + e.getMessage() + ")");
                    e.printStackTrace();
                }
            }
        });

        return files;
    }

    String params2 = "{ \n" +
            "    \"PropertyValues\": [\n" +
            "          {\n" +
            "            \"PropertyDef\": 100,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 29,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"PropertyDef\": 1068,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 9920,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"PropertyDef\": 1106,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 8135,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "       },\n" +
            "        {\n" +
            "            \"PropertyDef\": 1021,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 10,\n" +
            "                \"Lookups\": [\n" +
            "                    {\n" +
            "                        \"Item\": 3,\n" +
            "                        \"Version\": -1\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "          {\n" +
            "            \"PropertyDef\": 1027,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 10,\n" +
            "                \"Lookups\": [\n" +
            "                    {\n" +
            "                       \"Item\": 1,\n" +
            "                        \"Version\": -1\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "         {\n" +
            "            \"PropertyDef\": 1293,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 50,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"PropertyDef\": 1061,\n" +
            "            \"TypedValue\": {\n" +
            "                \"Value\": \"Site Inspection Record\",\n" +
            "                \"DataType\": 1\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"Files\": [\n" +
            "       \n" +
            "    ]\n" +
            "}";
    String paramsImage = "{ \n" +
            "    \"PropertyValues\": [\n" +
            "          {\n" +
            "            \"PropertyDef\": 100,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 40,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"PropertyDef\": 1068,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 9,\n" +
            "                \"Lookup\": {\n" +
            "                    \"Item\": 9920,\n" +
            "                    \"Version\": -1\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "   {" +
            "    \"PropertyDef\": 1027," +
            "    \"TypedValue\": {" +
            "         \"DataType\": 10," +
            "         \"Lookups\": [{" +
            "              \"Item\": 1," +
            "              \"Version\": -1" +
            "         }]" +
            "    }" +
            "},\n" +
            "        {\n" +
            "            \"PropertyDef\": 1033,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 10,\n" +
            "                \"Lookups\": [\n" +
            "                    {\n" +
            "                        \"Item\": 1,\n" +
            "                        \"Version\": -1\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "          {\n" +
            "            \"PropertyDef\": 1067,\n" +
            "            \"TypedValue\": {\n" +
            "                \"DataType\": 1,\n" +
            "                \"Value\": \"Bob\" \n" +
            "            }\n" +
            "        }\n" +
            "], \n" +
            "    \"Files\": [\n" +
            "       \n" +
            "    ]\n" +
            "}";

    String params = "{ " +
            "    \"PropertyValues\": [" +
            "          {" +
            "            \"PropertyDef\": {{p-class}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 9,\"" +
            "                \"Lookup\": {" +
            "                    \"Item\": 29,\"" +
            "                    \"Version\": -1" +
            "                }" +
            "            }" +
            "        }," +
            "        {" +
            "            \"PropertyDef\": {{p-project}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 9,\"" +
            "                \"Lookup\": {" +
            "                    \"Item\": 9920,\"" +
            "                    \"Version\": -1" +
            "                }" +
            "            }" +
            "        }," +
            "        {" +
            "            \"PropertyDef\": {{p-phase}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 9,\"" +
            "                \"Lookup\": {" +
            "                    \"Item\": 8135,\"" +
            "                    \"Version\": -1" +
            "                }" +
            "            }" +
            "       }," +
            "        {" +
            "            \"PropertyDef\": {{p-teams}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 10,\"" +
            "                \"Lookups\": [" +
            "                    {" +
            "                        \"Item\": 3,\"" +
            "                        \"Version\": -1" +
            "                    }" +
            "                ]" +
            "            }" +
            "        }," +
            "          {" +
            "            \"PropertyDef\": {{p-siteVisit}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 10,\"" +
            "                \"Lookups\": [" +
            "                    {" +
            "                       \"Item\": 1,\"" +
            "                        \"Version\": -1" +
            "                    }" +
            "                ]" +
            "            }" +
            "        }," +
            "         {" +
            "            \"PropertyDef\": {{p-recordType}},\"" +
            "            \"TypedValue\": {" +
            "                \"DataType\": 9,\"" +
            "                \"Lookup\": {" +
            "                    \"Item\": 50,\"" +
            "                    \"Version\": -1" +
            "                }" +
            "            }" +
            "        }," +
            "        {" +
            "            \"PropertyDef\": {{p-description}},\"" +
            "            \"TypedValue\": {" +
            "                \"Value\": \"Testing Site Inspection Form\",\"" +
            "                \"DataType\": 1" +
            "            }" +
            "        }" +
            "    ]," +
            "    \"Files\": [" +
            "       " +
            "    ]" +
            "}";

}
