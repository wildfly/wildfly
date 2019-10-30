/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import javax.ejb.TimerHandle;
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
