package lde.kiwi.mfiles;

import java.io.IOException;
import java.sql.SQLException;

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

import org.json.JSONException;
import org.json.JSONObject;

import io.agroal.api.AgroalDataSource;
import tech.bletchleypark.exceptions.InvalidSessionException;
import tech.bletchleypark.session.Session;
import tech.bletchleypark.session.SessionManager;
import tech.bletchleypark.tools.JSONObjectTools;

@Path("/mfiles")
public class MFilesAPI {
    @Inject
    SessionManager sessionsMgr;

    @Inject
    public AgroalDataSource defaultDataSource;

    private Session checkSessionIsVaild(UriInfo ui, HttpHeaders httpHeader)
            throws SQLException, InvalidSessionException {
        Session session = sessionsMgr.getSession(httpHeader, ui);
        return sessionsMgr.isValid(session)
                .machineId()
                .check() ? session : null;
    }

    @GET
    @Path("/alias")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response alias(@Context UriInfo ui,
            @Context HttpHeaders httpHeader) throws JSONException, IOException, SQLException, InvalidSessionException {
        checkSessionIsVaild(ui, httpHeader);
        return Response.ok(JSONObjectTools
                .JSONObjectNew("mfiles_alias_map", MFilesAliasManger.instanceOf.fetchAllAsJsonObject()).toString())
                .build();
    }

    @GET
    @Path("/site_visits")
    @Produces(MediaType.APPLICATION_JSON)
    public Response siteVisits(@QueryParam("vault") String vault,
            @QueryParam("object_id") int obj,
            @QueryParam("alias") String alias,
            @QueryParam("params") String params,
            @QueryParam("allProperties") String allProperties,
            @QueryParam("refresh") String refresh,
            @QueryParam("limit") int limit,
            @QueryParam("modifiedOnly") String modifiedOnly,
            @Context UriInfo ui,
            @Context HttpHeaders httpHeader) throws SQLException, InvalidSessionException {
        checkSessionIsVaild(ui, httpHeader);
        JSONObject response = new JSONObject();
        try {
            int query = vault.equals("project") ? 1092 : 1468;
            response = new MFiles().fetchSiteVisits(vault, obj, query, alias, params,
                    allProperties != null ? true : false, refresh != null ? true : false, limit,
                    modifiedOnly == null ? true : false);
            response.put("metadata_card", new MFiles().fetchCard(vault, obj, alias));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return Response.status(500).entity(e.getMessage()).build();
        }
        return Response.ok(response.toString()).build();
    }

}
