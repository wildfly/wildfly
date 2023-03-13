package org.wildfly.test.integration.microprofile.restclient.client;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient()
@RegisterClientHeaders()
@Path("api")
@Produces(MediaType.APPLICATION_JSON)
public interface InfoResource {


    @GET
    @Path("infos")
    @ClientHeaderParam(name="MyClientHeader", value = "newHeaderValue")
    @Retry()
    Response getInfos();

}