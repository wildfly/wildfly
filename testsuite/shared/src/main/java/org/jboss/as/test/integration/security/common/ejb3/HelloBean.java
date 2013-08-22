/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.security.common.ejb3;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

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
     * @see org.jboss.as.test.integration.security.common.ejb3.Hello#sayHelloWorld()
     */
    public String sayHelloWorld() {
        return HELLO_WORLD;
    }

    /**
     * Returns greeting with name retrieved from {@link SessionContext#getCallerPrincipal()}.
     *
     * @see org.jboss.as.test.integration.security.common.ejb3.Hello#sayHello()
     */
    public String sayHello() {
        return "Hello " + context.getCallerPrincipal().getName() + "!";
    }

}
