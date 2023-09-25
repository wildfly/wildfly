/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.nocreate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the EJB is not constructed if the chain does not complete
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
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
        Assert.assertEquals("Intercepted", bean.getMessage());
        Assert.assertFalse(AroundConstructSLSB.constructed);
    }

}
