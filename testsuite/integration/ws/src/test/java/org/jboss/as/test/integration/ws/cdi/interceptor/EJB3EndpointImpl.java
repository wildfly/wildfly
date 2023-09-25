/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.cdi.interceptor;

import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(name = "EJB3Endpoint", serviceName = "EJB3EndpointService", targetNamespace = "http://org.jboss.test.ws/jbws3441")
@Stateless
public class EJB3EndpointImpl implements EndpointIface {
    static boolean interceptorCalled;

    @EJBInterceptor
    public String echo(final String message) {
        return interceptorCalled ? message + " (including EJB interceptor)" : message;
    }
}
