/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.basic;

import jakarta.jws.WebService;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@WebService
public interface EndpointIface {

    String helloString(String input);

    HelloObject helloBean(HelloObject input);

    HelloObject[] helloArray(HelloObject[] input);

    String helloError(String input);
}
