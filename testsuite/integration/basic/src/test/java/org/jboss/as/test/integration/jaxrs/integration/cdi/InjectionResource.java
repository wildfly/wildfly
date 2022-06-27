/*
 * Copyright 2022 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jaxrs.integration.cdi;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/inject")
@Produces(MediaType.TEXT_PLAIN)
public class InjectionResource {

    @Inject
    private Application app;
    @Inject
    private CDIProvider provider;
    @Inject
    private CDIResource resource;

    @GET
    @Path("/app")
    public Response app() {
        if (app == null) {
            return Response.serverError().entity("Application was null").build();
        }
        return Response.ok(app.getClass().getName()).build();
    }

    @GET
    @Path("/provider")
    public Response provider() {
        if (provider == null) {
            return Response.serverError().entity("Provider was null").build();
        }
        return Response.ok(provider.getClass().getName()).build();
    }

    @GET
    @Path("/resource")
    public Response resource() {
        if (resource == null) {
            return Response.serverError().entity("Resource was null").build();
        }
        return Response.ok(resource.getClass().getName()).build();
    }
}
