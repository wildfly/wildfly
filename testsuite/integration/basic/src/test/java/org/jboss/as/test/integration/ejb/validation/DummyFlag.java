/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public interface DummyFlag {
    void setExecutedServiceCallFlag(boolean flag);

    @GET
    @Path("executed/mark")
    void markAsExecuted();

    @GET
    @Path("executed/clear")
    void clearExecution();

    @GET
    @Path("executed/status")
    public boolean getExecutedServiceCallFlag();
}
