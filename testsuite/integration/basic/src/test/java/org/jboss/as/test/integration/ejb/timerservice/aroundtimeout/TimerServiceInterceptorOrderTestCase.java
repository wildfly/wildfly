/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

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
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class TimerServiceInterceptorOrderTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceInterceptorOrder.war");
        war.addPackage(TimerServiceInterceptorOrderTestCase.class.getPackage());
        war.addAsWebInfResource(TimerServiceInterceptorOrderTestCase.class.getPackage(), "beans.xml", "beans.xml");
        return war;

    }

    @Test
    public void testArountTimeoutInterceptorOrder() throws NamingException {
        InterceptorOrder.reset();
        InitialContext ctx = new InitialContext();
        BeanChild bean = (BeanChild) ctx.lookup("java:module/" + BeanChild.class.getSimpleName());
        bean.createTimer();
        Assertions.assertTrue(BeanParent.awaitTimerCall());
        InterceptorOrder.assertEquals(InterceptorParent.class, InterceptorChild.class, MethodInterceptorParent.class, MethodInterceptorChild.class, CDIInterceptor.class, BeanParent.class, BeanChild.class);

    }

}
