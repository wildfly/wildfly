/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ee.globaldirectory.deployments;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibrary;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibraryImpl3;

/**
 * @author Tomas Terem (tterem@redhat.com)
 **/
@Path("global-directory")
public class GlobalDirectoryDeployment3 {

    @Path("/get")
    @GET
    @Produces("text/plain")
    public String get() {
        GlobalDirectoryLibrary globalDirectoryLibrary = new GlobalDirectoryLibraryImpl3();
        return globalDirectoryLibrary.get();
    }
}
