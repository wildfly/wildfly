/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule;

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
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SimpleScheduleFirstTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return createDeployment(SimpleScheduleFirstTestCase.class);
    }

    public static Archive<?> createDeployment(final Class<?> testClass) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testSchedule.war");
        war.addClasses(SimpleScheduleBean.class, SimpleSchedulesBean.class, SingletonScheduleBean.class);
        war.addClass(testClass);
        return war;

    }

    @Test
    public void testScheduleAnnotation() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleScheduleBean bean = (SimpleScheduleBean) ctx.lookup("java:module/" + SimpleScheduleBean.class.getSimpleName());
        Assert.assertTrue(SimpleScheduleBean.awaitTimerCall());

        Assert.assertEquals("info", bean.getTimerInfo());
        Assert.assertEquals("Europe/Prague", bean.getTimezone());
        Assert.assertTrue(bean.isCalendar());
        Assert.assertFalse(bean.isPersistent());

        final SingletonScheduleBean singletonBean = (SingletonScheduleBean) ctx.lookup("java:module/" + SingletonScheduleBean.class.getSimpleName());
        Assert.assertTrue(SingletonScheduleBean.awaitTimerCall());
    }

    @Test
    public void testScheduleTimezone() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleScheduleBean bean = (SimpleScheduleBean) ctx.lookup("java:module/" + SimpleScheduleBean.class.getSimpleName());
        bean.verifyTimezone();
    }

    @Test
    public void testSchedulesAnnotation() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleSchedulesBean bean = (SimpleSchedulesBean) ctx.lookup("java:module/" + SimpleSchedulesBean.class.getSimpleName());
        Assert.assertTrue(SimpleSchedulesBean.awaitTimerCall());
        Assert.assertEquals(5, bean.getTimers().size());

        Assert.assertEquals("info", bean.getTimerInfo());
        Assert.assertTrue(bean.isCalendar());
        Assert.assertTrue(bean.isPersistent());
    }


}
