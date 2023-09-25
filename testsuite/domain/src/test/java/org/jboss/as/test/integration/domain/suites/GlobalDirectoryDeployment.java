/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.suites;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Tomas Terem (tterem@redhat.com)
 **/
@Path("global-directory")
public class GlobalDirectoryDeployment {

    @Path("/library")
    @GET
    @Produces("text/plain")
    public String get() {
        GlobalDirectoryLibrary globalDirectoryLibrary = new GlobalDirectoryLibraryImpl();
        return globalDirectoryLibrary.get();
    }
}
