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
package org.jboss.as.test.integration.ejb.timerservice.view;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that an @Timeout method works when it is not part of any bean views
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ViewTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceView.war");
        war.addPackage(ViewTimerServiceTestCase.class.getPackage());
        return war;

    }

    @Test
    public void testAnnotationTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        LocalInterface bean = (LocalInterface) ctx.lookup("java:module/" + AnnotationTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(AnnotationTimerServiceBean.awaitTimerCall());
    }

    @Test
    public void testTimedObjectTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        LocalInterface bean = (LocalInterface) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());
    }


}
