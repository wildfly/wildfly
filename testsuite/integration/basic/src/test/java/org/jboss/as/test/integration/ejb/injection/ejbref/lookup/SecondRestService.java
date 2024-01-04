/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/second")
public class SecondRestService {

    @EJB(lookup = "ejb:ejb-test-ear/ejb//RemoteInterfaceBean!org.jboss.as.test.integration.ejb.injection.ejbref.lookup.RemoteInterface")
    RemoteInterface remoteBean;

    @GET
    @Path("/text")
    @Produces
    public String getResultXML() {
        return String.valueOf(remoteBean.ping());
    }

}
