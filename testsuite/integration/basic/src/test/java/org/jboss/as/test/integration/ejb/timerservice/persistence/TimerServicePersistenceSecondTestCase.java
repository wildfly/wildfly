/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;

/**
 * Tests that an @Timeout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServicePersistenceSecondTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, TimerServicePersistenceFirstTestCase.ARCHIVE_NAME);
        war.addPackage(TimerServicePersistenceSecondTestCase.class.getPackage());
        return war;

    }

    /**
     * The timer should be restored and the method should timeout, even without setting up the timer in this deployment
     */
    @Test
    public void testTimerServiceCalled() throws NamingException {
        Assert.assertTrue(SimpleTimerServiceBean.awaitTimerCall());
    }

    /**
     * The timer should not be restored, as it was cancelled
     */
    @Test
    public void testTimerServiceNotCalled() throws NamingException {
        Assert.assertFalse(CancelledTimerServiceBean.quickAwaitTimerCall());
    }

    /**
     * The timer should not be restored, it was non-persistent one
     */
    @Test
    public void testTimerServiceNonPersistent() throws NamingException {
        Assert.assertFalse(NonPersistentTimerServiceBean.quickAwaitTimerCall());
    }

    @Test
    public void testPersistentCalendarTimer() throws NamingException {
        Assert.assertEquals(CalendarTimerServiceBean.MESSAGE, CalendarTimerServiceBean.awaitTimerCall());
    }
}
