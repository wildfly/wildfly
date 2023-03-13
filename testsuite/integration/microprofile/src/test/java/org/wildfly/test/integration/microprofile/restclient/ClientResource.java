package org.wildfly.test.integration.microprofile.restclient;


import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.eclipse.microprofile.rest.client.inject.RestClient;


import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.wildfly.test.integration.microprofile.restclient.client.InfoResource;

import java.util.logging.Logger;

@Path("client")
public class ClientResource {

    private static final Logger logger = Logger.getLogger(ClientResource.class.getName());

    @Inject
    @RestClient
    private InfoResource infoResource;

    @Inject
    @ConfigProperty(name = "org.eclipse.microprofile.rest.client.propagateHeaders", defaultValue = "n/a")
    private String propagation;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response performRemoteServiceCall(@Context HttpHeaders headers, @HeaderParam(value = "Accept-Language") String aLang) {
        logger.info("ClientResource called");
        Config config = ConfigProvider.getConfig();
        logger.info("Client headers: " + headers.getRequestHeaders().toString());
        Iterable<ConfigSource> configSources = config.getConfigSources();
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        JsonObjectBuilder mainObjectBuilder = Json.createObjectBuilder();
        mainObjectBuilder.add("Name", "ClientResource");
        mainObjectBuilder.add("org.eclipse.microprofile.rest.client.propagateHeaders", propagation);
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        requestHeaders.forEach((key, value) -> objectBuilder.add(key, value.toString()));
        mainObjectBuilder.add("IncomingRequestHeaders", objectBuilder);
        logger.info("Calling now Server resource...");
        Response infosResponse = infoResource.getInfos();
        mainObjectBuilder.add("ServerResponse", infosResponse.readEntity(JsonObject.class));
        return Response.ok(mainObjectBuilder.build()).build();
    }


}
