/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;

/**
 * Webservice endpoint implementation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@Stateless
@WebService(
        endpointInterface = "org.jboss.as.test.integration.ws.ejb.SimpleStatelessWebserviceEndpointIface",
        targetNamespace = "org.jboss.as.test.integration.ws.ejb",
        serviceName = "SimpleService"
)
public class SimpleStatelessWebserviceEndpointImpl implements SimpleStatelessWebserviceEndpointIface {

    @Resource
    WebServiceContext ctx;

    @Override
    public String echo(final String s) {
        if (ctx == null) { throw new RuntimeException("@Resource WebServiceContext not injected"); }
        return s;
    }

}
