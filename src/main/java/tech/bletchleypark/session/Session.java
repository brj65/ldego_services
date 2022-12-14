package tech.bletchleypark.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.annotations.Expose;

import io.agroal.api.AgroalDataSource;
import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.SystemLogger;
import tech.bletchleypark.SystemLogger.ErrorCode;
import tech.bletchleypark.enums.LogLevel;

import static tech.bletchleypark.ConfigProviderManager.*;

public class Session {

    public Algorithm algorithm = Algorithm.HMAC256("Your-HMAC-secret");
    public final String issuer = "LDEGo API";
    public final static String SESSION_KEY = "X-Session";
    public final int SESSION_EXPIRES_SECONDS = 1800; // 1800 sec = 30 minutes
    private DecodedJWT decodedJWT;

    private static Map<String, Object> header = new HashMap<>();

    @Expose
    public final String domain;
    @Expose
    public final DateTime created;
    @Expose
    public final String sessionId;
    @Expose
    private String jwt;
    @Expose
    private DateTime lastAccessed;
    @Expose
    private int instanceId;
    @Expose
    private String xMachineId = "";
    @Expose
    private String userAgent = "";
    @Expose
    private String xApplication = "";
    @Expose
    private String xVersion = "";
    @Expose
    private JSONObject other = new JSONObject();

    private JSONObject device = new JSONObject();

    public enum SessionState {
        CREATED, ACTIVE, SLEEPING, EXPIRED;

        public boolean notExpired() {
            return this != EXPIRED;
        }
    }

    public int getInstanceId() {
        return instanceId;
    }
public String getxMachineId() {
    return xMachineId;
}
    public static Session create(HttpHeaders httpHeader, UriInfo ui) {
        return new Session(httpHeader, ui);
    }

    public static Session create(ResultSet rst) throws SQLException {
        return new Session(rst);
    }

    private Session(ResultSet rst) throws SQLException {
        domain = rst.getString("domain");
        lastAccessed = new DateTime(rst.getTimestamp("last_accessed"));
        created = new DateTime(rst.getTimestamp("created"));
        sessionId = rst.getString("session_id");
        jwt = rst.getString("jwt");
        instanceId = rst.getInt("instance_id");
        decodedJWT = JWT.decode(jwt);
        xMachineId = rst.getString("machine_id");
        xApplication = rst.getString("application");
        xVersion = rst.getString("version");
        userAgent = rst.getString("user_agent");
        other = new JSONObject(rst.getString("other"));
    }

    private Session(HttpHeaders httpHeader, UriInfo ui) {
        domain = ui.getBaseUri().getHost();
        lastAccessed = created = DateTime.now();
        sessionId = UUID.randomUUID().toString();
        jwt = createJWTToken();
        decodedJWT = JWT.decode(jwt);
        xMachineId = httpHeader.getHeaderString("X-Machine-Id");
        xApplication = httpHeader.getHeaderString("X-Application");
        xVersion = httpHeader.getHeaderString("X-Version");
        userAgent = httpHeader.getHeaderString("User-Agent");
    }

    public NewCookie[] getCookies() {
        NewCookie[] cookies = new NewCookie[1];
        Cookie cookie = new Cookie(SESSION_KEY, getJWTToken(), "/", domain);
        NewCookie jwtCookie = new NewCookie(cookie);
        cookies[0] = jwtCookie;
        return cookies;
    }

    // JWT Processing
    public DecodedJWT getDecodedJWT() {
        if (decodedJWT == null && jwt != null)
            decodedJWT = JWT.decode(jwt);
        return decodedJWT;
    }

    public String createJWTToken() {
        try {
            String token = JWT.create()
                    .withHeader(header)
                    .withIssuer(issuer)
                    .withClaim("iat", DateTime.now().toDate())
                    .withClaim("exp", DateTime.now().plusSeconds(SESSION_EXPIRES_SECONDS).toDate())
                    .withClaim("sessionId", sessionId)
                    .sign(algorithm);
            return token;
        } catch (JWTCreationException exception) {
            // Invalid Signing configuration / Couldn't convert Claims.
        }
        return null;
    }

    public String getJWTToken() {
        return getDecodedJWT().getToken();
    }

    // Static
    public static DecodedJWT getJWT(HttpHeaders httpHeader) {

        if (httpHeader.getCookies().keySet().contains(SESSION_KEY)
                || httpHeader.getRequestHeaders().keySet().contains(SESSION_KEY)) {
            String jwt = httpHeader.getCookies().keySet().contains(SESSION_KEY)
                    ? httpHeader.getCookies().get(SESSION_KEY).getValue()
                    : httpHeader.getRequestHeader(SESSION_KEY).get(0);
            if (!jwt.isEmpty()) {
                try {
                    return JWT.decode(jwt);
                } catch (com.auth0.jwt.exceptions.JWTDecodeException ex) {
                    SystemLogger.getLogger(Session.class).builder()
                            .errorCode(ErrorCode.INVAILD_JWT)
                            .logLevel(LogLevel.WARN)
                            .message("Invaild JWT Token")
                            .extra(httpHeader.getCookies().keySet().contains(SESSION_KEY)
                                    ? httpHeader.getCookies().get(SESSION_KEY).getValue()
                                    : httpHeader.getRequestHeader(SESSION_KEY).get(0))
                            .log();
                }
            }
        }
        return null;
    }

    public void validate() {
    }

    public JSONObject JSONObject() {
        JSONObject json = new JSONObject();
        json.put("session_id", sessionId);
        json.put("created", created);
        json.put("last_accessed", lastAccessed);
        json.put("state", getSessionState().toString());
        return json;
    }

    public SessionState getSessionState() {

        if (lastAccessed.plusSeconds(optConfigInt("bpark.sessions.expires.after.seconds", 600)).isBeforeNow())
            return SessionState.EXPIRED;
        if (lastAccessed.plusSeconds(optConfigInt("bpark.sessions.sleep.after.seconds", 20)).isBeforeNow())
            return SessionState.SLEEPING;
        if (created.equals(lastAccessed))
            return SessionState.CREATED;
        return SessionState.ACTIVE;
    }

    public synchronized void createSystemSession(AgroalDataSource dataSource) {
        if (!optConfigBoolean("bpark.sessions.local.only", false)) {
            try {
                try (Connection connection = dataSource.getConnection();
                        PreparedStatement ps = connection
                                .prepareStatement("INSERT INTO system_sessions ("
                                        + "session_id"
                                        + ",domain"
                                        + ",created"
                                        + ",last_accessed"
                                        + ",jwt"
                                        + ",instance_id"
                                        + ",machine_id"
                                        + ",user_agent"
                                        + ",application"
                                        + ",version"
                                        + ",other"
                                        + ") "
                                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {

                    int pramIndex = 1;
                    ps.setString(pramIndex++, sessionId);
                    ps.setString(pramIndex++, domain);
                    ps.setTimestamp(pramIndex++, new Timestamp(created.getMillis()));
                    ps.setTimestamp(pramIndex++, new Timestamp(lastAccessed.getMillis()));
                    ps.setString(pramIndex++, jwt);
                    ps.setInt(pramIndex++, ApplicationLifecycle.application.getInstanceId());
                    ps.setString(pramIndex++, xMachineId);
                    ps.setString(pramIndex++, userAgent);
                    ps.setString(pramIndex++, xApplication);
                    ps.setString(pramIndex++, xVersion);
                    ps.setString(pramIndex++, other.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void updateSystemSession(AgroalDataSource dataSource) {
        lastAccessed = DateTime.now();
        if (!optConfigBoolean("bpark.sessions.local.only", false)) {
            try {
                try (Connection connection = dataSource.getConnection();
                        PreparedStatement ps = connection
                                .prepareStatement("UPDATE system_sessions SET "
                                        + "           last_accessed = ? "
                                        + "           ,jwt = ? "
                                        + "           ,instance_id=? "
                                        + "           ,machine_id=? "
                                        + "           ,user_agent=? "
                                        + "           ,application=? "
                                        + "           ,version=? "
                                        + "           ,other=? "
                                        + " WHERE session_id = ?")) {

                    int pramIndex = 1;
                    try {
                        ps.setTimestamp(pramIndex++, new Timestamp(lastAccessed.getMillis()));
                        ps.setString(pramIndex++, jwt);
                        ps.setInt(pramIndex++, ApplicationLifecycle.application.getInstanceId());
                        ps.setString(pramIndex++, xMachineId);
                        ps.setString(pramIndex++, userAgent);
                        ps.setString(pramIndex++, xApplication);
                        ps.setString(pramIndex++, xVersion);
                        ps.setString(pramIndex++, other.toString());

                        ps.setString(pramIndex++, sessionId);
                     if(   ps.executeUpdate()==0){
                        createSystemSession(dataSource);
                     }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void deleteSystemSession(AgroalDataSource dataSource) {
        if (!optConfigBoolean("bpark.sessions.local.only", false)) {
            try {
                try (Connection connection = dataSource.getConnection();
                        PreparedStatement ps = connection
                                .prepareStatement("DELETE FROM system_sessions WHERE session_id = ?")) {
                    ps.setString(1, sessionId);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Session fetch(String sessionId, AgroalDataSource dataSource) {
        try {

            try (Connection connection = dataSource.getConnection();
                    PreparedStatement ps = connection
                            .prepareStatement("SELECT * FROM system_sessions WHERE session_id = ?");) {
                try {
                    ps.setString(1, sessionId);
                    try (ResultSet rst = ps.executeQuery();) {
                        if (rst.next()) {
                            return create(rst);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setDevice(ResultSet rst) throws JSONException, SQLException {
        device = new JSONObject();
        device.put("pool", rst.getString("pool"));
        device.put("min_access_level", rst.getString("min_access_level"));
    }
    public JSONObject getDevice() {
        return device;
    }
}
