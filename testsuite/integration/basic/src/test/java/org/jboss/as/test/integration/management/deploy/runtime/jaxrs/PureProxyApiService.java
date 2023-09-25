/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@Path("pure/proxy")
public interface PureProxyApiService {

    @Path("test/{a}/{b}")
    @GET
    String test(@PathParam("a") String a, @PathParam("b") String b);
}
