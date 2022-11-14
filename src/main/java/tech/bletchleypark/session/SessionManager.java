package tech.bletchleypark.session;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import com.auth0.jwt.interfaces.DecodedJWT;

@ApplicationScoped
public class SessionManager {

    private final HashMap<String, Session> sessions = new HashMap<>();

    public enum SessionType {
        DEVICE, WEB
    }

    public Session getSession(HttpHeaders httpHeader, UriInfo ui) {
        DecodedJWT jwt = Session.getJWT(httpHeader);
        Session session = null;
        if (jwt != null) {
            session = sessions.get(jwt.getClaim("sessionId").asString());

            if (session != null)
                return session;
        }
        session = Session.createSession(httpHeader, ui);
        sessions.put(session.sessionId, session);
        return session;

    }

    public int countSessions(SessionType sessionType) {
        return sessions.size();
    }
}
