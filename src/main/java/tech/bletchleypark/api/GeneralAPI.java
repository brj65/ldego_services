package tech.bletchleypark.api;

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
import org.json.JSONArray;

import io.agroal.api.AgroalDataSource;
import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.beans.User;
import tech.bletchleypark.exceptions.InvalidSessionException;
import tech.bletchleypark.session.Session;
import tech.bletchleypark.session.SessionManager;
import tech.bletchleypark.session.Session.SessionState;

import static tech.bletchleypark.ConfigProviderManager.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Path("")
public class GeneralAPI {
    @Inject
    ApplicationLifecycle app;

    @Inject
    SessionManager sessionMgr;

    @Inject
    public AgroalDataSource defaultDataSource;

    @GET
    @Path("/info")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getInfo(@QueryParam("requestFor") String requestFor, @Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        Session session = sessionMgr.getSession(httpHeader, ui);
        String responce = "";
        switch (requestFor.toLowerCase()) {
            case "container":
                responce = optConfigString("container_host", "DEVELOPMENT") + "-"
                        + optConfigString("container_name", "LDEGO");
                break;
            case "sessions":
                responce = sessionMgr.countSessions(SessionState.SLEEPING) + "/"
                        + sessionMgr.countSessions(SessionState.EXPIRED) + "/"
                        + sessionMgr.countSessions(null);
                break;
            case "serverdatetime":
                responce = DateTime.now().toString("EE dd MMM yy kk:mm:ss z") + "  [" + app.getInstanceId() + "]";
                break;
        }
        return Response.ok(responce).cookie(session.getCookies()).build();
    }

    @GET
    @Path("/init")
    @Produces(MediaType.TEXT_PLAIN)
    public Response init(@QueryParam("requestFor") String requestFor, @Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        Session session = sessionMgr.getSession(httpHeader, ui);
        return Response.ok().cookie(session.getCookies()).build();
    }

    @GET
    @Path("status")
    @Produces(MediaType.TEXT_PLAIN)
    public Response status() {
        return Response.ok().build();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.TEXT_PLAIN)
    public Response users( @Context UriInfo ui,
    @Context HttpHeaders httpHeader) throws SQLException, InvalidSessionException{
        JSONArray users = new JSONArray();
        Session session = sessionMgr.getSession(httpHeader, ui);
        sessionMgr.isValid(session).machineId().check();
        try(Connection connection = defaultDataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM user ");
        ){
            ResultSet rst = ps.executeQuery();
            while(rst.next()){
                User user = new  User(rst);
                users.put( user.JSONObject());
            }
        }
        return Response.ok(app.toJSONObject("users", users).toString()).cookie(session.getCookies()).build();
    }
}
