/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import jakarta.jws.WebService;

/**
 * @author Rostislav Svoboda
 */
@WebService
public interface EJBEndpointIface {

    String hello(String input);

    String helloForAll(String input);

    String helloForNone(String input);

    String helloForRole(String input);

    String helloForRoles(String input);
}
