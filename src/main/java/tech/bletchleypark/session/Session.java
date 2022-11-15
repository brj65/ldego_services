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
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.annotations.Expose;

import io.agroal.api.AgroalDataSource;

import static tech.bletchleypark.ConfigProviderManager.*;
import static tech.bletchleypark.ApplicationLifecycle.*;

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

    public enum SessionState {
        CREATED, ACTIVE, SLEEPING, EXPIRED;

        public boolean notExpired() {
            return this != EXPIRED;
        }
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
    }

    private Session(HttpHeaders httpHeader, UriInfo ui) {
        domain = ui.getBaseUri().getHost();
        lastAccessed = created = DateTime.now();
        sessionId = UUID.randomUUID().toString();
        jwt = createJWTToken();
        decodedJWT = JWT.decode(jwt);
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
            return JWT.decode(httpHeader.getCookies().keySet().contains(SESSION_KEY)
                    ? httpHeader.getCookies().get(SESSION_KEY).getValue()
                    : httpHeader.getRequestHeader(SESSION_KEY).get(0));
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
                                        + ") "
                                        + " VALUES (?,?,?,?,?,?)")) {
                    int pramIndex = 1;
                    ps.setString(pramIndex++, sessionId);
                    ps.setString(pramIndex++, domain);
                    ps.setTimestamp(pramIndex++, new Timestamp(created.getMillis()));
                    ps.setTimestamp(pramIndex++, new Timestamp(lastAccessed.getMillis()));
                    ps.setString(pramIndex++, jwt);
                    ps.setInt(pramIndex++, application.getInstanceId());
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
                                        + "           last_accessed = ?"
                                        + "           ,jwt = ?"
                                        + "           ,instance_id=?"
                                        + " WHERE session_id = ?")) {
                    int pramIndex = 1;
                    try {
                        ps.setTimestamp(pramIndex++, new Timestamp(lastAccessed.getMillis()));
                        ps.setString(pramIndex++, jwt); 
                         ps.setInt(pramIndex++, application.getInstanceId());
                        ps.setString(pramIndex++, sessionId);                      
                        ps.executeUpdate();
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
}
