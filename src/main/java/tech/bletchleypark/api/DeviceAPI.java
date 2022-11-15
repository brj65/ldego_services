package tech.bletchleypark.api;

import java.io.IOException;
import java.sql.SQLException;

import javax.inject.Inject;
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

import tech.bletchleypark.exceptions.InvalidSessionException;
import tech.bletchleypark.session.Session;
import tech.bletchleypark.session.SessionManager;

@Path("/device")
public class DeviceAPI {
    @Inject
    SessionManager sessionsMgr;

    @GET
    @PUT
    @POST
    @Path("/call_home")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tasks(String json, @Context UriInfo ui,
            @Context HttpHeaders httpHeader, @QueryParam("pram") String pram, @QueryParam("jwt") String jwt)
            throws InvalidSessionException, IOException, SQLException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
       Session session = sessionsMgr.getSession(httpHeader, ui);   
    
    //    Device device = Device.getInstance(httpHeader.getHeaderString("X-MACHINE-ID"));
     //   if(device == null)                       //        return Response.status(401).entity("").build();
        return Response.ok(session.getJWTToken()).cookie(session.getCookies()).build();
    }
}
