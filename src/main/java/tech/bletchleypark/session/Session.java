package tech.bletchleypark.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.annotations.Expose;

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

    public static Session createSession(HttpHeaders httpHeader, UriInfo ui) {
        return new Session(httpHeader, ui);
    }

    private Session(HttpHeaders httpHeader, UriInfo ui) {
        domain = ui.getBaseUri().getHost();
        created = DateTime.now();
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
}
