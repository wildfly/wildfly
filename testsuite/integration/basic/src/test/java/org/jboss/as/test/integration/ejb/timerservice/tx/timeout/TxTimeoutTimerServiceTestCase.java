/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that transaction annotations are applied to timeout method regardless of it's access modifier.
 *
 * @author Tomasz Adamski
 */
@ExtendWith(ArquillianExtension.class)
public class TxTimeoutTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "Jar.jar");
        jar.addPackage(TxTimeoutTimerServiceTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testPublicTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimeoutBeanRemoteView bean = (TimeoutBeanRemoteView) ctx.lookup("java:module/"
                + PublicTxTimoutBean.class.getSimpleName());
        bean.startTimer();
        Assertions.assertTrue(PublicTxTimoutBean.awaitTimerCall());
        Assertions.assertEquals(5, PublicTxTimoutBean.getTimeout());
    }

    @Test
    public void testPrivateTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimeoutBeanRemoteView bean = (TimeoutBeanRemoteView) ctx.lookup("java:module/"
                + PrivateTxTimeoutBean.class.getSimpleName());
        bean.startTimer();
        Assertions.assertTrue(PrivateTxTimeoutBean.awaitTimerCall());
        Assertions.assertEquals(5, PrivateTxTimeoutBean.getTimeout());
    }

    @Test
    public void testPublicScheduleMethod() throws NamingException {
        Assertions.assertTrue(PublicTxScheduleBean.awaitTimerCall());
        Assertions.assertEquals(5, PublicTxScheduleBean.getTimeout());
    }

    @Test
    public void testPrivateScheduleMethod() throws NamingException {
        Assertions.assertTrue(PrivateTxScheduleBean.awaitTimerCall());
        Assertions.assertEquals(5, PrivateTxScheduleBean.getTimeout());
    }

}
