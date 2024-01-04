/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author olukas
 */
@DeclareRoles({HelloBean.AUTHORIZED_ROLE, HelloBean.NOT_AUTHORIZED_ROLE})
@Stateless
@RolesAllowed(HelloBean.AUTHORIZED_ROLE)
@Remote(Hello.class)
public class HelloBean implements Hello {

    public static final String AUTHORIZED_ROLE = "Admin";
    public static final String NOT_AUTHORIZED_ROLE = "User";

    public static final String HELLO = "Hello!";

    @Override
    public String sayHello() {
        return HELLO;
    }

}
