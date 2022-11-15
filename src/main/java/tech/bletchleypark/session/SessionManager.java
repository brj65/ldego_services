package tech.bletchleypark.session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;

import com.auth0.jwt.interfaces.DecodedJWT;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import tech.bletchleypark.SystemLogger;
import tech.bletchleypark.SystemLogger.ErrorCode;
import tech.bletchleypark.session.Session.SessionState;
import static tech.bletchleypark.ConfigProviderManager.*;

@ApplicationScoped
public class SessionManager {

    @Inject
    public AgroalDataSource defaultDataSource;

    SystemLogger logger = SystemLogger.getLogger(SessionManager.class);
    private final HashMap<String, Session> sessions = new HashMap<>();
    private Timer cleanup = new Timer("Clean Up Sessions");

    public enum SessionType {
        DEVICE, WEB
    }

    void onStart(@Observes StartupEvent ev) {
        cleanup.schedule(new TimerTask() {
            private boolean running = false;
            private int passes = 0;
            private int maxPasses = 1;

            @Override
            public void run() {
                if (running && passes < maxPasses) {
                    try {
                        logger.builder()
                                .message("Skipped, Session Cleanup is still running")
                                .warn()
                                .errorCode(ErrorCode.PROCESS_STILL_RUNNING)
                                .log();
                        passes++;
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                running = true;
                passes = 0;
                if (!optConfigBoolean("bpark.sessions.local.only", false))
                    try {
                        try (Connection connection = defaultDataSource.getConnection();
                                PreparedStatement ps = connection.prepareStatement("SELECT * FROM system_sessions");
                                ResultSet rst = ps.executeQuery()) {
                            while (rst.next()) {
                                Session session = Session.create(rst);
                                if (sessions.get(session.sessionId) == null) {
                                    sessions.put(session.sessionId, session);
                                }
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                List<Session> expiredSession = getSessions(SessionState.EXPIRED);
                if (!expiredSession.isEmpty())
                    logger.info("Removed " + expiredSession.size() + " sessions of " + countSessions());
                expiredSession.forEach(key -> {
                    removeSession(key);
                });
                running = false;
            }

        }, 1 * 1000,
                optConfigInt("bpark.sessions.clean.up.every.seconds", 60) * 1000);
    }

    private void removeSession(Session session) {
        sessions.remove(session.sessionId);
        session.deleteSystemSession(defaultDataSource);
    }

    private void addSession(Session session) {
        sessions.put(session.sessionId, session);
        session.createSystemSession(defaultDataSource);
    }

    public Session getSession(HttpHeaders httpHeader, UriInfo ui) {
        DecodedJWT jwt = Session.getJWT(httpHeader);
        Session session = null;
        if (jwt != null) {
            session = sessions.get(jwt.getClaim("sessionId").asString());
            if (session != null && session.getSessionState().notExpired()) {
                session.updateSystemSession(defaultDataSource);
                return session;
            }
        }
        session = Session.create(httpHeader, ui);
        addSession(session);
        return session;
    }

    public List<String> getSessionKeys(SessionState sessionState) {
        HashMap<String, Session> currentSessions = new HashMap<>(sessions);
        Map<String, Session> filtered = currentSessions.entrySet()
                .stream()
                .filter(session -> sessionState == null || session.getValue().getSessionState() == sessionState)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ArrayList<String>(filtered.keySet());
    }

    public List<Session> getSessions(SessionState sessionState) {
        HashMap<String, Session> currentSessions = new HashMap<>(sessions);
        Map<String, Session> filtered = currentSessions.entrySet()
                .stream()
                .filter(session -> sessionState == null || session.getValue().getSessionState() == sessionState)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ArrayList<Session>(filtered.values());
    }

    public int countSessions(SessionState state) {
        return getSessionKeys(state).size();
    }

    public int countSessions() {
        return sessions.size();
    }

    public JSONArray getSessions() {
        JSONArray array = new JSONArray();
        for (Session session : sessions.values()) {
            array.put(session.JSONObject());
        }
        return array;
    }

}
