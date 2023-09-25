/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider.preference;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

@Path("/user")
@Produces("text/plain")
public class Resource {

    @Context
    private Providers providers;

    @GET
    public boolean getUser() {
        MessageBodyWriter<?> writer = providers.getMessageBodyWriter(Object.class, Object.class, new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        return writer instanceof CustomMessageBodyWriter;
    }
}
