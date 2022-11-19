package tech.bletchleypark.devices;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import io.agroal.api.AgroalDataSource;
import tech.bletchleypark.exceptions.InvalidSessionException;
import tech.bletchleypark.session.Session;
import tech.bletchleypark.session.SessionManager;

@Path("/device")
public class DeviceAPI {
    @Inject
    SessionManager sessionsMgr;

    @Inject
    public AgroalDataSource defaultDataSource;

    @GET
    @Path("autoAuth")
    @Produces(MediaType.APPLICATION_JSON)
    public Response autoAuth(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) throws SQLException, InvalidSessionException {
        JSONObject responce = new JSONObject();
        boolean approved = false;
        Session session = sessionsMgr.getSession(httpHeader, ui);
        sessionsMgr.isValid(session).check();
        try (Connection connection = defaultDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("SELECT approved FROM device "
                        + " WHERE machine_id = ?")) {
            ps.setString(1, session.getxMachineId());
            ResultSet rst = ps.executeQuery();
            if (rst.next()) {
                approved = rst.getBoolean("approved");
            }
            rst.close();
        }
        responce.put("auth", approved);
        return Response.ok(responce.toString()).cookie(session.getCookies()).build();
    }

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRegister(String json2, @Context UriInfo ui,
            @Context HttpHeaders httpHeader,
            @QueryParam("jwt") String jwt,
            @CookieParam("authToken") String authToken) throws Exception, InvalidSessionException {
        JSONObject responseJson = new JSONObject();
        Session session = sessionsMgr.getSession(httpHeader, ui);
        if (session == null)
            throw new InvalidSessionException();
        JSONObject requestJson = new JSONObject(json2);
        try (Connection connection = defaultDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("INSERT INTO device "
                        + "(machine_id,brand,model,operating_system,operating_system_version"
                        + ",application_id,application_version) "
                        + "VALUES (?,?,?,?,?,?,?)")) {
            int cnt = 1;
            ps.setString(cnt++, requestJson.optString("machine_id"));
            ps.setString(cnt++, requestJson.optString("brand"));
            ps.setString(cnt++, requestJson.optString("model"));
            ps.setString(cnt++, requestJson.optString("operating_system"));
            ps.setString(cnt++, requestJson.optString("operating_system_version"));
            ps.setString(cnt++, requestJson.optString("application_id"));
            ps.setString(cnt++, requestJson.optString("application_version"));
            ps.executeUpdate();
        }
        return Response.ok(responseJson.toString()).build();
    }

    @GET
    @PUT
    @POST
    @Path("/call_home")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tasks(String json, @Context UriInfo ui,
            @Context HttpHeaders httpHeader, @QueryParam("pram") String pram, @QueryParam("jwt") String jwt)
            throws SQLException {
        Session session = sessionsMgr.getSession(httpHeader, ui);
        try {
            sessionsMgr.isValid(session).machineId().check();
        } catch (SQLException e) {
            throw e;
        } catch (InvalidSessionException e) {
            sessionsMgr.removeSession(session);
            return Response.ok(e.getMessage()).cookie(session.getCookies()).build();
        }
        JSONObject responce = new JSONObject();
        responce.put("jwt", session.getJWTToken());
        responce.put("config", session.getDevice());
        JSONArray requests = new JSONArray();
        responce.put("requests", requests);
        return Response.ok(responce.toString()).cookie(session.getCookies()).build();
    }
}
