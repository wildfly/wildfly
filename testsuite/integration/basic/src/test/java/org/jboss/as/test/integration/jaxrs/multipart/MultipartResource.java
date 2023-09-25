/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.multipart;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

/**
 * @author Stuart Douglas
 */
@Path("/form")
public class MultipartResource {

    @GET
    @Produces("multipart/related")
    public MultipartFormDataOutput get() {
        MultipartFormDataOutput output = new MultipartFormDataOutput();
        output.addPart("Hello", MediaType.TEXT_PLAIN_TYPE);
        output.addPart("World", MediaType.TEXT_PLAIN_TYPE);
        return output;
    }
}
