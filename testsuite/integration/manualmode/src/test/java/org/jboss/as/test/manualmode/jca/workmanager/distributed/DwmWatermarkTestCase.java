/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests distributed work manager and whether it really distributes work over multiple nodes. Test cases use two servers
 * both with a deployed resource adapter configured to use the DWM. Tests with WATERMARK policy.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DwmWatermarkTestCase extends AbstractDwmTestCase {

    private static final int WATERMARK_MAX_THREADS = 2;

    @Override
    protected Policy getPolicy() {
        return Policy.WATERMARK;
    }

    @Override
    protected Selector getSelector() {
        return Selector.MAX_FREE_THREADS;
    }

    @Override
    protected int getWatermarkPolicyOption() {
        return WATERMARK_MAX_THREADS - 1;
    }

    @Override
    protected int getSrtMaxThreads() {
        return WATERMARK_MAX_THREADS;
    }

    @Override
    protected int getSrtQueueLength() {
        return WATERMARK_MAX_THREADS;
    }

    /**
     * Schedules several (one more than our max threads) long work instances and verifies that
     * {@link org.jboss.jca.core.api.workmanager.DistributedWorkManager#scheduleWork(Work)} schedules the work on both
     * nodes. Policy.WATERMARK will select the local node once, then we hit the watermark limit, and the other node is
     * selected.
     */
    @Test
    public void testWatermarkPolicy() throws WorkException, InterruptedException {
        int scheduleWorkAcceptedServer1 = server1Proxy.getScheduleWorkAccepted();
        int scheduleWorkAcceptedServer2 = server2Proxy.getScheduleWorkAccepted();
        int distributedScheduleWorkAccepted = server1Proxy.getDistributedScheduleWorkAccepted();

        for (int i = 0; i < WATERMARK_MAX_THREADS; i++) {
            server1Proxy.scheduleWork(new LongWork());
        }

        Assert.assertTrue("Work did not finish in the expected time on the expected node",
                waitForScheduleWork(server2Proxy, scheduleWorkAcceptedServer2 + 1, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT)));
        Assert.assertTrue("Work did not finish in the expected time on the expected node",
                waitForScheduleWork(server1Proxy, scheduleWorkAcceptedServer1 + 1, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT)));
        Assert.assertTrue("Expected distributedScheduleWorkAccepted = " + (distributedScheduleWorkAccepted + WATERMARK_MAX_THREADS)
                        + " but was: " + server2Proxy.getDistributedScheduleWorkAccepted(),
                server2Proxy.getDistributedScheduleWorkAccepted() == distributedScheduleWorkAccepted + WATERMARK_MAX_THREADS);
    }
}
