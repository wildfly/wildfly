/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.ejb;

import jakarta.jws.WebService;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService
public interface ContextHandlerEndpointIface {
    String doSomething(String msg);
}
