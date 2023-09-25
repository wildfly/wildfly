/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jaxrs;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;


@Path("interception/resource")
@ComponentInterceptorBinding
public class JaxRsResource {

    public static boolean injectionOK = false;

    @Inject
    private Alpha alpha;

    @Inject
    public void setBravo(Bravo bravo) {
        injectionOK = (alpha != null) && (bravo != null);
    }

    @GET
    @Produces({ "text/plain" })
    public String getMessage() {
        return "Hello";
    }

    @GET
    @Path("/componentInterceptor/numberOfInterceptions")
    @Produces({ "text/plain" })
    public Integer getComponentInterceptorIntercepts() {
        return ComponentInterceptor.getInterceptions().size();
    }

    @GET
    @Path("componentInterceptor/firstInterception")
    @Produces({ "text/plain" })
    public String getFirstInterceptionMethodName() {
        return ComponentInterceptor.getInterceptions().get(0).getMethodName();
    }

    @GET
    @Path("/injectionOk")
    @Produces({ "text/plain" })
    public Boolean getResourceInjectionBool() {
        return injectionOK;
    }

}
