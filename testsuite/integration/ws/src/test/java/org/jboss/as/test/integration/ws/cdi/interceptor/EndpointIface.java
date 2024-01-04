/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.cdi.interceptor;

import jakarta.jws.WebService;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@WebService(name = "EndpointIface", targetNamespace = "http://org.jboss.test.ws/jbws3441")
public interface EndpointIface {
    String echo(final String message);
}
