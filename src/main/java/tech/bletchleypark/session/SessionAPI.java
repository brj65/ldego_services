package tech.bletchleypark.session;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.SystemLogger;
import tech.bletchleypark.exceptions.InvalidSessionException;

@Path("")
public class SessionAPI {
    SystemLogger logger = SystemLogger.getLogger(SessionAPI.class);
    @Inject
    SessionManager sessionMgr;
    @Inject
    ApplicationLifecycle application;

    @GET
    @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getInfo(@QueryParam("shh") String shh, @Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        if (!application.shhRequest(shh)) {
            Session session = sessionMgr.getSession(httpHeader, ui);
            session.validate();
        }
        return sessionMgr.getSessions();
    }

    @GET
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToken(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) throws InvalidSessionException {
        Session session = sessionMgr.getSession(httpHeader, ui);
        if (session.getxMachineId().isEmpty())
            throw new InvalidSessionException();
        JSONObject result = new JSONObject();
        result.put("jwt", session.getJWTToken());
        return Response.ok(result.toString()).cookie(session.getCookies()).build();
    }

}
