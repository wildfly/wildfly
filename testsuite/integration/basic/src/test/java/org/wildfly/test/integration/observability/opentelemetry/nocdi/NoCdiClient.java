package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

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
