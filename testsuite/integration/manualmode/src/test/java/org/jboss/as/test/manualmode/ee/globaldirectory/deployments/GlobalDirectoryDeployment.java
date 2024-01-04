/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ee.globaldirectory.deployments;

import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibrary;
import org.jboss.as.test.manualmode.ee.globaldirectory.libraries.GlobalDirectoryLibraryImpl;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Tomas Terem (tterem@redhat.com)
 **/
@Path("global-directory")
public class GlobalDirectoryDeployment {

   @Path("/get")
   @GET
   @Produces("text/plain")
   public String get() {
      GlobalDirectoryLibrary globalDirectoryLibrary = new GlobalDirectoryLibraryImpl();
      return globalDirectoryLibrary.get();
   }
}
