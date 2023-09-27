/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.simple;

import java.util.Date;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
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

        //verifies that timer1 equals itself and does not equal null
        Assert.assertTrue(timer1.equals(timer1));
        Assert.assertFalse(timer1.equals(null));

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
