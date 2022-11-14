package tech.bletchleypark.apis;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import static tech.bletchleypark.ConfigProviderManager.*;
@Path("/info")
public class GeneralAPI {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getInfo(@QueryParam("requestFor") String requestFor) {
        String responce = "";
        switch (requestFor.toLowerCase()) {
            case "container":
                responce = optConfigString("container_host", "DEVELOPMENT")+"-"+optConfigString("container_name", "LDEGO");
                break;
            case "sessions":
                responce = "0/0";
                break;
            case "serverdatetime":
                responce = DateTime.now().toString("EE dd MMM yy kk:hh:ss z");
                break;
        }
        return Response.ok(responce).build();
    }
}
