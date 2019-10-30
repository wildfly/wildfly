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
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Uses policy = ALWAYS and selector = FIRST_AVAILABLE.
 *
 * InSequence is necessary since we can't tell whether a work instance has already finished executing and has freed all
 * the threads it was occupying (if started via startWork or scheduleWork). The last two test cases could mess with
 * each other. The test methods will still run separately without issues.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DwmAlwaysTestCase extends AbstractDwmTestCase {

    @Override
    protected Policy getPolicy() {
        return Policy.ALWAYS;
    }

    @Override
    protected Selector getSelector() {
        return Selector.FIRST_AVAILABLE;
    }

    /**
     * Executes a few short work instances and verifies that they executed on the expected node (the other one since
     * we have Policy.ALWAYS)
     */
    @Test
    @InSequence(1)
    public void testDoWork() throws WorkException, InterruptedException {
        int doWorkAccepted = server2Proxy.getDoWorkAccepted();
        int distributedDoWorkAccepted = server1Proxy.getDistributedDoWorkAccepted();

        server1Proxy.doWork(new ShortWork());
        server1Proxy.doWork(new ShortWork());

        Assert.assertTrue("Work did not finish in the expected time on the expected node",
                waitForDoWork(server2Proxy, doWorkAccepted + 2, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT)));
        Assert.assertTrue("Expected distributedDoWorkAccepted = " + (distributedDoWorkAccepted + 2) + " but was: " + server1Proxy.getDistributedDoWorkAccepted(),
                server1Proxy.getDistributedDoWorkAccepted() == distributedDoWorkAccepted + 2);
    }

    /**
     * Submits a few (less than our max threads) short work instances and verifies that
     * {@link org.jboss.jca.core.api.workmanager.DistributedWorkManager#startWork(Work)} starts the work on the
     * expected node (the other one since we have Policy.ALWAYS).
     */
    @Test
    @InSequence(2)
    public void testStartWork() throws WorkException, InterruptedException {
        int startWorkAccepted = server2Proxy.getStartWorkAccepted();
        int distributedStartWorkAccepted = server1Proxy.getDistributedStartWorkAccepted();

        server1Proxy.startWork(new ShortWork());

        // the short work was already started and will finish momentarily, however, it still hogs the thread for testScheduleWork
        // this will allow it to finish for good (short work takes no time to finish)
        Thread.sleep(TimeoutUtil.adjust(1000));

        Assert.assertTrue("Work did not finish in the expected time on the expected node",
                waitForStartWork(server2Proxy, startWorkAccepted + 1, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT)));
        Assert.assertTrue("Expected distributedStartWorkAccepted = " + (distributedStartWorkAccepted + 1) + " but was: " + server1Proxy.getDistributedStartWorkAccepted(),
                server1Proxy.getDistributedStartWorkAccepted() == distributedStartWorkAccepted + 1);
    }

    /**
     * Schedules several (one more than our max threads) long work instances and verifies that
     * {@link org.jboss.jca.core.api.workmanager.DistributedWorkManager#scheduleWork(Work)} returns sooner than the time
     * needed for the work items to actually finish. Also verifies that work was executed on both nodes (Policy.ALWAYS
     * selects the other node first, then the first node is selected because the second one is already full).
     */
    @Test
    @InSequence(3)
    public void testScheduleWork() throws WorkException, InterruptedException {
        int scheduleWorkAcceptedServer1 = server1Proxy.getScheduleWorkAccepted();
        int scheduleWorkAcceptedServer2 = server2Proxy.getScheduleWorkAccepted();
        int distributedScheduleWorkAccepted = server1Proxy.getDistributedScheduleWorkAccepted();

        for (int i = 0; i < SRT_MAX_THREADS + 1; i++) {
            server1Proxy.scheduleWork(new LongWork());
        }

        Assert.assertTrue("Work did not finish in the expected time on the expected node",
                waitForScheduleWork(server2Proxy, scheduleWorkAcceptedServer2 + SRT_MAX_THREADS, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT * (SRT_MAX_THREADS + 1))));
        Assert.assertTrue("Work was not scheduled on nodes as was expected",
                waitForScheduleWork(server1Proxy, scheduleWorkAcceptedServer1 + 1, TimeoutUtil.adjust(WORK_FINISH_MAX_TIMEOUT * (SRT_MAX_THREADS + 1))));
        Assert.assertTrue("Expected distributedScheduleWorkAccepted = " + (distributedScheduleWorkAccepted + SRT_MAX_THREADS + 1) + " but was: " + server1Proxy.getDistributedScheduleWorkAccepted(),
                server1Proxy.getDistributedScheduleWorkAccepted() == distributedScheduleWorkAccepted + SRT_MAX_THREADS + 1);
    }
}
