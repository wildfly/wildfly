/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Providers;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("provider-check")
@Produces(MediaType.APPLICATION_JSON)
public class ProviderCheckResource {

    @Inject
    private Providers providers;

    @GET
    @Path("reader")
    public JsonObject checkReader(@QueryParam("type") String type, @QueryParam("genericType") final String genericType, @QueryParam("mediaType") final MediaType mediaType) {
        return generateJson(providers.getMessageBodyReader(find(type), find(genericType), null, mediaType == null ? MediaType.WILDCARD_TYPE : mediaType));
    }

    @GET
    @Path("writer")
    public JsonObject checkWriter(@QueryParam("type") String type, @QueryParam("genericType") final String genericType, @QueryParam("mediaType") final MediaType mediaType) {
        return generateJson(providers.getMessageBodyWriter(find(type), find(genericType), null, mediaType == null ? MediaType.WILDCARD_TYPE : mediaType));
    }

    @GET
    @Path("exception-mapper")
    public JsonObject checkExceptionMapper(@QueryParam("type") String type) {
        return generateJson(providers.getExceptionMapper(find(type, Throwable.class)));
    }

    private JsonObject generateJson(final Object type) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        if (type != null) {
            builder.add("type", type.getClass().getName());
        } else {
            builder.addNull("type");
        }
        return builder.build();
    }

    private Class<?> find(final String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException("Failed to find type " + name, e);
        }
    }

    private <T> Class<? extends T> find(final String name, final Class<T> baseType) {
        try {
            Class<?> result = Class.forName(name);
            if (baseType.isAssignableFrom(result)) {
                return (Class<? extends T>) result;
            }
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException("Failed to find type " + name, e);
        }
        throw new WebApplicationException("Failed to find type " + name);
    }
}
