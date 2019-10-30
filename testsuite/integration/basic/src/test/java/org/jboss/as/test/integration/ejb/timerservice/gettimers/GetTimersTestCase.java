/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
 *
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;
import java.util.Map;

import javax.ejb.Timer;
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
