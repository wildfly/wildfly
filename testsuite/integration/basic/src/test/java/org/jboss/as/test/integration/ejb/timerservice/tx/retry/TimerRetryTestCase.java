/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.retry;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class TimerRetryTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "timerRetryTestCase.war");
        war.addPackage(TimerRetryTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testRollbackRetry() throws NamingException {
        InitialContext ctx = new InitialContext();
        RollbackRetryBean bean = (RollbackRetryBean) ctx.lookup("java:module/" + RollbackRetryBean.class.getSimpleName());
        bean.createTimer();
        Assertions.assertTrue(RollbackRetryBean.awaitTimerCall());
    }

    @Test
    public void testExceptionRetry() throws NamingException {
        InitialContext ctx = new InitialContext();
        ExceptionRetryBean bean = (ExceptionRetryBean) ctx.lookup("java:module/" + ExceptionRetryBean.class.getSimpleName());
        bean.createTimer();
        Assertions.assertTrue(ExceptionRetryBean.awaitTimerCall());
    }



}
