package tech.bletchleypark.session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

import tech.bletchleypark.ApplicationLifecycle;
import tech.bletchleypark.SystemLogger;

import static tech.bletchleypark.ConfigProviderManager.*;

@Path("system")
public class SystemAPI {
    SystemLogger logger = SystemLogger.getLogger(SystemAPI.class);
    @Inject
    SessionManager sessionMgr;
    @Inject
    ApplicationLifecycle application;

    @GET
    @Path("config/datasource")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getDtatsoureceConfig(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        return application.getDefaultDataSourceConfig();
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getConfig(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        return application.getConfig();
    }

    @GET
    @Path("status/datasource")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDatasource(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) {
        Session session = sessionMgr.getSession(httpHeader, ui);
        session.validate();
        /*
         * dsc.put("db-kind", optConfigString("quarkus.datasource.db-kind", ""));
         * dsc.put("username", optConfigString("quarkus.datasource.username", ""));
         * dsc.put("password", optConfigString("quarkus.datasource.password", ""));
         * dsc.put("url", optConfigString("quarkus.datasource.jdbc.url", ""));
         * dsc.put("leak-detection-interval",
         * optConfigString("quarkus.datasource.jdbc.leak-detection-interval", ""));
         * dsc.put("enable-metrics",
         * optConfigString("quarkus.datasource.jdbc.enable-metrics", ""));
         * dsc.put("mix_size", optConfigString("quarkus.datasource.jdbc.min-size", ""));
         * dsc.put("max_size", optConfigString("quarkus.datasource.jdbc.max-size", ""));
         * dsc.put("inital_size",
         * optConfigString("quarkus.datasource.jdbc.initial-size", ""));
         * dsc.put("new-connection-sql",
         * optConfigString("quarkus.datasource.jdbc.new-connection-sql", ""));
         * dsc.put("acquisition-timeout",optConfigString(
         * "quarkus.datasource.jdbc.acquisition-timeout",""));
         */

        String url = optConfigString("quarkus.datasource.jdbc.url", "");
        String username = optConfigString("quarkus.datasource.username", "");
        String password = optConfigString("quarkus.datasource.password", "");
        logger.info("Testing DB Connection to " + url + " with user " + username);
        logger.info("Connecting database...");
        
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            logger.info("Testing DB Connection to " + url + " with user " + username); 
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
        logger.info("Passed");
        System.out.println("Testing DB Connection to " + url + " with user " + username);
        return Response.ok().build();
    }
}
