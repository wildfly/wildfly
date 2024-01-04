/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.integration.cdi;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("cdiInject")
@Produces({"text/plain"})
public class CDIResource {

    @Inject
    CDIBean bean;

    @GET
    public String getMessage() {
        return bean.message();
    }
}
