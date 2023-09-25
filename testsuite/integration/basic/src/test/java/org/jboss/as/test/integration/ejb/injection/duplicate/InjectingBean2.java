/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.duplicate;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Stuart Douglas
 */
@Stateless
public class InjectingBean2 {

    @EJB(name = "java:global/a")
    public Bean1 bean1;

    public Bean1 getBean() {
        return bean1;
    }

    public Bean1 lookupGlobalBean() throws NamingException {
        return (Bean1)new InitialContext().lookup("java:global/a" );
    }
}
