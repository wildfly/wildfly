/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.threadpool;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ejb3ThreadReuseTestCase - start EJB singleton with 1 sec timer and check that single thread is reused
 * <p>
 *
 * @author Miroslav Novak
 */
@RunWith(Arquillian.class)
@ServerSetup(Ejb3ThreadReuseTestCase.ServerSetup.class)
public class Ejb3ThreadReuseTestCase extends Ejb3ThreadPoolBase {

    private static final int MAX_THREADS = 10;
    private static final int CORE_THREADS = 0;
    private static final long KEEEP_ALIVE_TIME = 100;
    private static final String KEEP_ALIVE_TIME_UNIT = "SECONDS";

    @Test
    public void testNonCoreThreadReused() throws Exception {
        ScheduleSingletonOneTimer scheduleSingletonLocal = (ScheduleSingletonOneTimer) iniCtx.lookup("java:module/"
                + ScheduleSingletonOneTimer.class.getSimpleName() + "!" + ScheduleSingletonOneTimer.class.getName());

        // check that at least 3 tasks were processed by thread pool and then verify that there is still just one thread
        waitUntilThreadPoolProcessedAtLeast(2, 10000);

        Assert.assertEquals("Number of threads in pool must be 1.", 1,
                readAttribute("current-thread-count").asInt());

        // verify that it's still the same thread
        Assert.assertEquals("There is always different thread processing tasks but it should be one.",
                1, scheduleSingletonLocal.getThreadNames().size());
    }

    public static class ServerSetup extends SnapshotRestoreSetupTask {
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            // create one
            ModelNode writeMaxThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads", MAX_THREADS);
            ModelNode writeCoreThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads", CORE_THREADS);
            ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                    new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", KEEP_ALIVE_TIME_UNIT)
                            .add("time", KEEEP_ALIVE_TIME)));

            Ejb3ThreadPoolBase.executeOperation(writeMaxThreadsOp);
            Ejb3ThreadPoolBase.executeOperation(writeCoreThreadsOp);
            Ejb3ThreadPoolBase.executeOperation(writeKeepAliveTimeOp);
        }
    }
}
