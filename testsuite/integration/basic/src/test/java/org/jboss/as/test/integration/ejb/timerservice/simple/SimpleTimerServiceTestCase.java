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
package org.jboss.as.test.integration.ejb.timerservice.simple;

import java.util.Date;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an @Timeout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SimpleTimerServiceTestCase {

    private static int TIMER_INIT_TIME_MS = 100;
    private static int TIMER_TIMEOUT_TIME_MS = 100;

    private static String INFO_MSG_FOR_CHECK = "info";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(SimpleTimerServiceTestCase.class.getPackage());
        return war;
    }

    @Test
    @InSequence(1)
    public void testAnnotationTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        AnnotationTimerServiceBean bean = (AnnotationTimerServiceBean) ctx.lookup("java:module/" + AnnotationTimerServiceBean.class.getSimpleName());
        bean.resetTimerServiceCalled();
        bean.getTimerService().createTimer(TIMER_TIMEOUT_TIME_MS, INFO_MSG_FOR_CHECK);
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());

        bean.resetTimerServiceCalled();
        long ts = (new Date()).getTime() + TIMER_INIT_TIME_MS;
        bean.getTimerService().createTimer(new Date(ts), INFO_MSG_FOR_CHECK);
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());

        Assert.assertEquals(INFO_MSG_FOR_CHECK, bean.getTimerInfo());
        Assert.assertFalse(bean.isCalendar());
        Assert.assertTrue(bean.isPersistent());
    }

    @Test
    @InSequence(2)
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

    @Test
    @InSequence(3)
    public void testIntervalTimer() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(INFO_MSG_FOR_CHECK);

        AnnotationTimerServiceBean bean1 = (AnnotationTimerServiceBean) ctx.lookup("java:module/" + AnnotationTimerServiceBean.class.getSimpleName());
        bean1.resetTimerServiceCalled();
        long ts = (new Date()).getTime() + TIMER_INIT_TIME_MS;
        Timer timer1 = bean1.getTimerService().createIntervalTimer(new Date(ts), TIMER_TIMEOUT_TIME_MS, timerConfig);
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());
        bean1.resetTimerServiceCalled();
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());
        timer1.cancel();

        TimedObjectTimerServiceBean bean2 = (TimedObjectTimerServiceBean) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean2.resetTimerServiceCalled();
        Timer timer2 = bean2.getTimerService().createIntervalTimer(TIMER_INIT_TIME_MS, TIMER_TIMEOUT_TIME_MS, timerConfig);
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());
        bean2.resetTimerServiceCalled();
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());
        timer2.cancel();
    }

}
