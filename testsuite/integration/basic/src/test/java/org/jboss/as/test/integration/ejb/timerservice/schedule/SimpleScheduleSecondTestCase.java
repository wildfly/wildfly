/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
