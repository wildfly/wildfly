package org.wildfly.test.integration.microprofile.restclient;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.logging.Logger;

@Path("infos")
public class ServerResource {

    private static final Logger logger = Logger.getLogger(ServerResource.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfos(@Context HttpHeaders headers) {
        logger.info("ServerResource called");
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        logger.info("Recieved Headers: " + headers.getRequestHeaders().toString());
        JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
        mainObjectBuilder.add("Name", "ServerResource");
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        requestHeaders.forEach((key, value) -> objectBuilder.add(key, value.toString()));
        mainObjectBuilder.add("IncomingRequestHeaders", objectBuilder);
        return Response.ok(mainObjectBuilder.build()).build();
    }
}