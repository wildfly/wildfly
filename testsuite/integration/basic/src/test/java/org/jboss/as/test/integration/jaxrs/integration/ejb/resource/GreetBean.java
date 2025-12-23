/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb.resource;

import jakarta.ejb.Local;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Local
public interface GreetBean {

    @Path("greet")
    @GET
    String greet();
}
