/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.GZIP;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("echo")
public class EchoResource {

    @Inject
    private HttpHeaders httpHeaders;

    @POST
    @Path("/text")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String echoText(final String echo) {
        return echo;
    }

    @POST
    @Path("/xml")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public String echoXml(final String echo) {
        return echo;
    }

    @POST
    @Path("/simple-text")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public SimpleText echoSimpleText(final SimpleText echo) {
        return echo;
    }

    @GET
    @Path("/simple-text")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public SimpleText simpleText() {
        // Accept types
        final List<String> acceptMediaTypes = httpHeaders.getAcceptableMediaTypes().stream()
                .map(MediaType::toString)
                .collect(Collectors.toList());
        if (acceptMediaTypes.isEmpty()) {
            return new SimpleText("*/*");
        }
        return new SimpleText(acceptMediaTypes.get(0));
    }

    @POST
    @Path("/gzip")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @GZIP
    public Response echoGzip(@GZIP final String echo) {
        return Response.ok(echo).build();
    }

    @POST
    @Path("/form/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response echoForm(final Form form) {
        if (form.asMap().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("error", "no form data").build())
                    .build();
        }
        return Response.ok(Json.createObjectBuilder(form.asMap()).build()).build();
    }

    @POST
    @Path("/form/map")
    @Produces(MediaType.APPLICATION_JSON)
    public Response echoForm(final MultivaluedMap<String, String> form) {
        if (form.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder().add("error", "no form data").build())
                    .build();
        }
        return Response.ok(Json.createObjectBuilder(form).build()).build();
    }

    @GET
    @Path("/headers")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject headers() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonObjectBuilder headerBuilder = Json.createObjectBuilder();
        // Add the headers
        httpHeaders.getRequestHeaders()
                .forEach((name, value) -> headerBuilder.add(name, Json.createArrayBuilder(value)));
        builder.add("headers", headerBuilder);

        // Accepted languages
        final List<String> languages = httpHeaders.getAcceptableLanguages().stream()
                .map(Locale::toLanguageTag)
                .filter(languageTag -> !(languageTag.equalsIgnoreCase("und")))
                .collect(Collectors.toList());
        if (!languages.isEmpty()) {
            builder.add("acceptedLanguages", Json.createArrayBuilder(languages));
        }
        // Accept types
        final List<String> acceptMediaTypes = httpHeaders.getAcceptableMediaTypes().stream()
                .map(MediaType::toString)
                .collect(Collectors.toList());
        if (!acceptMediaTypes.isEmpty()) {
            builder.add("acceptedMediaTypes", Json.createArrayBuilder(acceptMediaTypes));
        }
        return builder.build();
    }
}
