/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.cdi.interceptor;

import jakarta.jws.WebService;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(name = "POJOEndpoint", serviceName = "POJOEndpointService", targetNamespace = "http://org.jboss.test.ws/jbws3441")
public class POJOEndpointImpl implements EndpointIface {
    static boolean interceptorCalled;

    @POJOInterceptor
    public String echo(final String message) {
        return interceptorCalled ? message + " (including POJO interceptor)" : message;
    }
}
