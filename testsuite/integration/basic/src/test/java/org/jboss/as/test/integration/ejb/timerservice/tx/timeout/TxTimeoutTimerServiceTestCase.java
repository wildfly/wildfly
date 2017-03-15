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
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that transaction annotations are applied to timeout method regardless of it's access modifier.
 *
 * @author Tomasz Adamski
 */
@RunWith(Arquillian.class)
public class TxTimeoutTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "Jar.jar");
        jar.addPackage(TxTimeoutTimerServiceTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.jts \n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void testPublicTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimeoutBeanRemoteView bean = (TimeoutBeanRemoteView) ctx.lookup("java:module/"
                + PublicTxTimoutBean.class.getSimpleName());
        bean.startTimer();
        Assert.assertTrue(PublicTxTimoutBean.awaitTimerCall());
        Assert.assertEquals(5, PublicTxTimoutBean.getTimeout());
    }

    @Test
    public void testPrivateTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimeoutBeanRemoteView bean = (TimeoutBeanRemoteView) ctx.lookup("java:module/"
                + PrivateTxTimeoutBean.class.getSimpleName());
        bean.startTimer();
        Assert.assertTrue(PrivateTxTimeoutBean.awaitTimerCall());
        Assert.assertEquals(5, PrivateTxTimeoutBean.getTimeout());
    }

    @Test
    public void testPublicScheduleMethod() throws NamingException {
        Assert.assertTrue(PublicTxScheduleBean.awaitTimerCall());
        Assert.assertEquals(5, PublicTxScheduleBean.getTimeout());
    }

    @Test
    public void testPrivateScheduleMethod() throws NamingException {
        Assert.assertTrue(PrivateTxScheduleBean.awaitTimerCall());
        Assert.assertEquals(5, PrivateTxScheduleBean.getTimeout());
    }

}
