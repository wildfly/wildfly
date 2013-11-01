/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.injection.support.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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