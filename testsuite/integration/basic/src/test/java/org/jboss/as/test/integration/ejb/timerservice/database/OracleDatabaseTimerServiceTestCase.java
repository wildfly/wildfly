/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.timerservice.database;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.TimerConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;

/**
 * Tests @Timeout method called with the Oracle database persistent timer
 * Test for [ WFLY-16290 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(OracleDatabaseTimerServerSetup.class)
public class OracleDatabaseTimerServiceTestCase {

    private static int TIMER_INIT_TIME_MS = 100;
    private static int TIMER_TIMEOUT_TIME_MS = 100;
    private static String INFO_MSG_FOR_CHECK = "info";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testOracleTimer.war");
        war.addPackage(OracleDatabaseTimerServiceTestCase.class.getPackage());
        war.addAsWebInfResource(OracleDatabaseTimerServiceTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return war;
    }

    @Test
    public void testTimedObjectTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimedObjectTimerServiceBean bean = (TimedObjectTimerServiceBean) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean.resetTimerServiceCalled();
        bean.getTimerService().createTimer(TIMER_TIMEOUT_TIME_MS, INFO_MSG_FOR_CHECK);
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        bean.resetTimerServiceCalled();
        long ts = (new Date()).getTime() + TIMER_INIT_TIME_MS;
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(INFO_MSG_FOR_CHECK);
        bean.getTimerService().createSingleActionTimer(new Date(ts), timerConfig);
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        Assert.assertEquals(INFO_MSG_FOR_CHECK, bean.getTimerInfo());
        Assert.assertFalse(bean.isCalendar());
        Assert.assertTrue(bean.isPersistent());
    }
}
