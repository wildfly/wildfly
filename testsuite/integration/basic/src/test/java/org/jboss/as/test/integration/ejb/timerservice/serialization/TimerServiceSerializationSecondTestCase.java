/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.serialization;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an @Timout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServiceSerializationSecondTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return TimerServiceSerializationFirstTestCase.createTestArchive(TimerServiceSerializationSecondTestCase.class);
    }

    /**
     * The timer should be restored and the method should timeout, even without setting up the timer in this deployment
     */
    @Test
    public void testTimerServiceRestoredWithCorrectInfo() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimerServiceSerializationBean bean = (TimerServiceSerializationBean) ctx.lookup("java:module/" + TimerServiceSerializationBean.class.getSimpleName());
        InfoA info = TimerServiceSerializationBean.awaitTimerCall();
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.infoB);
        Assert.assertNotNull(info.infoB.infoC);
    }



}
