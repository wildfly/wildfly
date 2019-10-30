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
package org.jboss.as.test.integration.ejb.timerservice.overlap;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@RunWith(Arquillian.class)
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
        Assert.assertTrue("Schedule timeout method was not invoked within 3 seconds!", ScheduleRetryFailSingletonBean.startLatch().await(TIMER_CALL_WAITING1_S, TimeUnit.SECONDS));
        Assert.assertTrue("Schedule timer is not alive after overlapped invocation!",ScheduleRetryFailSingletonBean.aliveLatch().await(TIMER_CALL_WAITING2_S, TimeUnit.SECONDS));
    }
}
