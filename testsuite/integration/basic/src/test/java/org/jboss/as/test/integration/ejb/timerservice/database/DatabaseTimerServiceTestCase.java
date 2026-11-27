/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.database;

import java.util.Date;
import jakarta.ejb.TimerConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that an @Timeout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(DatabaseTimerServerSetup.class)
public class DatabaseTimerServiceTestCase {

    private static int TIMER_INIT_TIME_MS = 100;
    private static int TIMER_TIMEOUT_TIME_MS = 100;

    private static String INFO_MSG_FOR_CHECK = "info";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(DatabaseTimerServiceTestCase.class.getPackage());
        war.addAsWebInfResource(DatabaseTimerServiceTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return war;
    }


    @Test
    public void testTimedObjectTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimedObjectTimerServiceBean bean = (TimedObjectTimerServiceBean) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean.resetTimerServiceCalled();
        bean.getTimerService().createTimer(TIMER_TIMEOUT_TIME_MS, INFO_MSG_FOR_CHECK);
        Assertions.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        bean.resetTimerServiceCalled();
        long ts = (new Date()).getTime() + TIMER_INIT_TIME_MS;
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(INFO_MSG_FOR_CHECK);
        bean.getTimerService().createSingleActionTimer(new Date(ts), timerConfig);
        Assertions.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        Assertions.assertEquals(INFO_MSG_FOR_CHECK, bean.getTimerInfo());
        Assertions.assertFalse(bean.isCalendar());
        Assertions.assertTrue(bean.isPersistent());
    }

}
