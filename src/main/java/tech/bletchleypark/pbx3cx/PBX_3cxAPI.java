package tech.bletchleypark.pbx3cx;

import java.io.IOException;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/pbx")
public class PBX_3cxAPI {
    @GET
    @Path("import/extentions")
    public Response mfileImportUsersFromFile(@Context UriInfo ui, @Context HttpHeaders httpHeader,
            @QueryParam("pram") String pram) throws  SQLException, IOException {
        PBX_3cx.doImport();
        return Response.ok().build();
    }
}
