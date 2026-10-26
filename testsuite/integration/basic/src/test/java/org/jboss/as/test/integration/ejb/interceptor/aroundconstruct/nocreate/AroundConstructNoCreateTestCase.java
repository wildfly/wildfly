/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.nocreate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the EJB is not constructed if the chain does not complete
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class AroundConstructNoCreateTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testlocal.war");
        war.addPackage(AroundConstructNoCreateTestCase.class.getPackage());
        return war;
    }


    @Test
    public void testAroundConstructNoCreate() throws NamingException {
        InitialContext ctx = new InitialContext();
        AroundConstructSLSB bean = (AroundConstructSLSB) ctx.lookup("java:module/" + AroundConstructSLSB.class.getSimpleName());
        Assertions.assertEquals("Intercepted", bean.getMessage());
        Assertions.assertFalse(AroundConstructSLSB.constructed);
    }

}
