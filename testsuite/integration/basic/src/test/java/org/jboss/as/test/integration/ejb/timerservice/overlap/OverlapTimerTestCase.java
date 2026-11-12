/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.overlap;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@ExtendWith(ArquillianExtension.class)
public class OverlapTimerTestCase {

    private static final int TIMER_CALL_WAITING1_S = 3;
    private static final int TIMER_CALL_WAITING2_S = 5;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "overlap-timer-test.jar");
        ejbJar.addPackage(OverlapTimerTestCase.class.getPackage());
        return ejbJar;
    }

    @Test
    public void testContinuedAfterOverlap() throws Exception {
        Assertions.assertTrue(ScheduleRetryFailSingletonBean.startLatch().await(TIMER_CALL_WAITING1_S, TimeUnit.SECONDS), "Schedule timeout method was not invoked within 3 seconds!");
        Assertions.assertTrue(ScheduleRetryFailSingletonBean.aliveLatch().await(TIMER_CALL_WAITING2_S, TimeUnit.SECONDS),"Schedule timer is not alive after overlapped invocation!");
    }
}
