/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.schedule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that persistent timers created out of @Schedule work fine
 *
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class SimpleScheduleSecondTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return SimpleScheduleFirstTestCase.createDeployment(SimpleScheduleSecondTestCase.class);
    }

    /**
     * The timer should be restored and the @Schedule must timeout
     */
    @Test
    public void testScheduleMethodTimeout() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleScheduleBean bean = (SimpleScheduleBean)ctx.lookup("java:module/" + SimpleScheduleBean.class.getSimpleName());
        Assert.assertTrue(SimpleScheduleBean.awaitTimerCall());

        final SingletonScheduleBean singletonBean = (SingletonScheduleBean) ctx.lookup("java:module/" + SingletonScheduleBean.class.getSimpleName());
        Assert.assertTrue(SingletonScheduleBean.awaitTimerCall());

    }
}
