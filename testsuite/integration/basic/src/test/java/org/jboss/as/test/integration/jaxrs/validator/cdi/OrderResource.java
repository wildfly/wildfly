/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator.cdi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Produces("text/plain")
@Path("order/{id}")
public class OrderResource {

    @GET
    public OrderModel get(@PathParam("id") @CustomMax int id) {
        return new OrderModel(id);
    }
}
