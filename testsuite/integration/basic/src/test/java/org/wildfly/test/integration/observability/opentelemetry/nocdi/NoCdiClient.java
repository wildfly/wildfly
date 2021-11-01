package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("/client")
public class NoCdiClient {
    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getJson(@Context UriInfo uriInfo) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            return client.target(uriInfo.getBaseUriBuilder().path("json"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
