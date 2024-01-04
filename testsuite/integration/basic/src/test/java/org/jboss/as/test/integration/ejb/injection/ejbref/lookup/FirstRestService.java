/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/first")
public class FirstRestService {

    @EJB(lookup = "java:comp/env/RemoteInterfaceBean")
    RemoteInterface remoteBean;

    @GET
    @Path("/text")
    @Produces
    public String getResultXML() {
        return String.valueOf(remoteBean.ping());
    }

}
