/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;
import java.util.Map;

import jakarta.ejb.Timer;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for https://issues.jboss.org/browse/WFLY-5221
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@RunWith(Arquillian.class)
public class GetTimersTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "gettimers.jar");
        jar.addPackage(GetTimersTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testGetTimers() throws NamingException {
        InitialContext ctx = new InitialContext();
        StartupBean starterBean = (StartupBean) ctx.lookup("java:module/" + StartupBean.class.getSimpleName());
        AbstractTimerBean bean1 = (AbstractTimerBean) ctx.lookup("java:module/" + TimerBeanOne.class.getSimpleName());
        AbstractTimerBean bean2 = (AbstractTimerBean) ctx.lookup("java:module/" + TimerBeanTwo.class.getSimpleName());

        try {
            Map<String, Collection<Timer>> beanTimersMap = starterBean.startTimers();
            assertTimersPrefix(beanTimersMap.get(TimerBeanOne.class.getSimpleName()), TimerBeanOne.class.getSimpleName());
            assertTimersPrefix(beanTimersMap.get(TimerBeanTwo.class.getSimpleName()), TimerBeanTwo.class.getSimpleName());
        } finally {
            bean1.stopTimers();
            bean2.stopTimers();

            Assert.assertEquals(0, bean1.getTimers().size());
            Assert.assertEquals(0, bean2.getTimers().size());
        }
    }

    public void assertTimersPrefix(Collection<Timer> timers, String bean) {
        for (Timer timer : timers) {
            Assert.assertTrue(String.format("Bean %s returned timer %s which doesn't belong to this bean.",
                            bean, timer.getInfo()),
                    timer.getInfo().toString().startsWith(bean));
        }
    }
}
