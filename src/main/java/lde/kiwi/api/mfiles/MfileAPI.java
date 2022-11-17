package lde.kiwi.api.mfiles;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lde.kiwi.mfiles.MFilesUsers;

@Path("/mfiles")
public class MfileAPI {
    @GET
    @Path("import/users")
    public Response mfileImportUsersFromFile(@Context UriInfo ui,@Context HttpHeaders httpHeader, @QueryParam("pram") String pram) throws IOException, SQLException {
        MFilesUsers.doImport(Paths.get("/opt","mfiles","mFile_Users.txt" ));
        return Response.ok().build();
    }
}
