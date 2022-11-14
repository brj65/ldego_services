package tech.bletchleypark.apis;

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

import org.joda.time.DateTime;

import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.session.Session;
import tech.bletchleypark.session.SessionManager;
import tech.bletchleypark.session.SessionManager.SessionType;

import static tech.bletchleypark.ConfigProviderManager.*;

@Path("")
public class GeneralAPI {

    @Inject
    ApplicationLifecycle app;
    @Inject
    SessionManager sessionMgr;

    @GET
    @Path("/info")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getInfo(@QueryParam("requestFor") String requestFor, @Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        Session session = sessionMgr.getSession(httpHeader,ui);
        String responce = "";
        switch (requestFor.toLowerCase()) {
            case "container":
                responce = optConfigString("container_host", "DEVELOPMENT") + "-"
                        + optConfigString("container_name", "LDEGO");
                break;
            case "sessions":
                responce = sessionMgr.countSessions(SessionType.WEB) + "/"
                        + sessionMgr.countSessions(SessionType.DEVICE);
                break;
            case "serverdatetime":
                responce = DateTime.now().toString("EE dd MMM yy kk:hh:ss z");
                break;
        }
        return Response.ok(responce).cookie(session.getCookies()).build();
    }

    @GET
    @Path("/init")
    @Produces(MediaType.TEXT_PLAIN)
    public Response init(@QueryParam("requestFor") String requestFor, @Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        Session session = sessionMgr.getSession(httpHeader,ui);
        return Response.ok().cookie(session.getCookies()).build();
    }
}
