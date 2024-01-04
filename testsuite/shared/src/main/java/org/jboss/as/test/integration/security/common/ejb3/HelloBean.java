/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.ejb3;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * A simple implementation of {@link Hello} interface. It's annotated as a {@link Stateless} bean with {@link Hello} as a
 * {@link Remote remote} interface. Access to the methods is protected and only {@value #ROLE_ALLOWED} role has access.
 *
 * @author Josef Cacek
 */
@DeclareRoles(HelloBean.ROLE_ALLOWED)
@Stateless
@RolesAllowed(HelloBean.ROLE_ALLOWED)
@Remote(Hello.class)
public class HelloBean implements Hello {

    public static final String ROLE_ALLOWED = "TestRole";

    public static final String HELLO_WORLD = "Hello world!";

    @Resource
    private SessionContext context;

    // Public methods --------------------------------------------------------

    /**
     * Returns {@value #HELLO_WORLD}.
     *
     * @see Hello#sayHelloWorld()
     */
    public String sayHelloWorld() {
        return HELLO_WORLD;
    }

    /**
     * Returns greeting with name retrieved from {@link SessionContext#getCallerPrincipal()}.
     *
     * @see Hello#sayHello()
     */
    public String sayHello() {
        return "Hello " + context.getCallerPrincipal().getName() + "!";
    }

}
