/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.security;

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
 * Tests that {@link jakarta.ejb.EJBContext#getCallerPrincipal()} returns the unauthenticated identity in a timeout method.
 *
 * AS7-3154
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServiceCallerPrincipleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceGetCallerPrinciple.war");
        war.addPackage(TimerServiceCallerPrincipleTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testGetCallerPrincipleInTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimerGetCallerPrincipleBean bean = (TimerGetCallerPrincipleBean) ctx.lookup("java:module/" + TimerGetCallerPrincipleBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertEquals("anonymous", TimerGetCallerPrincipleBean.awaitTimerCall());
    }


}
