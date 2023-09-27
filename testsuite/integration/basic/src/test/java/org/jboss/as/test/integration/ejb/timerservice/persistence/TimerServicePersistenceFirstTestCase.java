/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import jakarta.ejb.TimerHandle;
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
 * Two phase test for timer serialization. This test case creates a persistent timer, and the second phase restores it.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServicePersistenceFirstTestCase {

    /**
     * must match between the two tests.
     */
    public static final String ARCHIVE_NAME = "testTimerServicePersistence.war";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(TimerServicePersistenceFirstTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testSimplePersistentTimer() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleTimerServiceBean bean = (SimpleTimerServiceBean)ctx.lookup("java:module/" + SimpleTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(SimpleTimerServiceBean.awaitTimerCall());
    }

    @Test
    public void testPersistentCalendarTimer() throws NamingException {
        InitialContext ctx = new InitialContext();
        CalendarTimerServiceBean bean = (CalendarTimerServiceBean)ctx.lookup("java:module/" + CalendarTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertEquals(CalendarTimerServiceBean.MESSAGE, CalendarTimerServiceBean.awaitTimerCall());
    }

    @Test
    public void createAndCancelTimerService() throws NamingException {
        InitialContext ctx = new InitialContext();
        CancelledTimerServiceBean bean = (CancelledTimerServiceBean)ctx.lookup("java:module/" + CancelledTimerServiceBean.class.getSimpleName());
        TimerHandle handle = bean.createTimer();
        Assert.assertTrue(CancelledTimerServiceBean.awaitTimerCall());
        Assert.assertEquals("info", handle.getTimer().getInfo());
        handle.getTimer().cancel();
    }

    @Test
    public void createNonPersistentTimer() throws NamingException {
        InitialContext ctx = new InitialContext();
        NonPersistentTimerServiceBean bean = (NonPersistentTimerServiceBean)ctx.lookup("java:module/" + NonPersistentTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(NonPersistentTimerServiceBean.quickAwaitTimerCall());
    }


}
