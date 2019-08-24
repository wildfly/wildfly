/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.threadpool;


import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ejb3NonCoreThreadTimeoutTestCase - start EJB singleton with 1 sec timer and check that threads timeout
 * <p>
 *
 * @author Miroslav Novak
 */
@RunWith(Arquillian.class)
@ServerSetup(Ejb3NonCoreThreadTimeoutTestCase.ServerSetup.class)
public class Ejb3NonCoreThreadTimeoutTestCase extends Ejb3ThreadPoolBase {

    private static final int MAX_THREADS = 10;
    private static final int CORE_THREADS = 0;
    private static final long KEEEP_ALIVE_TIME = 10;
    private static final String KEEP_ALIVE_TIME_UNIT = "MILLISECONDS";

    @Test
    public void testThreadTimeout() throws Exception {
        ScheduleSingletonOneTimer scheduleSingletonLocal = (ScheduleSingletonOneTimer) iniCtx.lookup("java:module/"
                + ScheduleSingletonOneTimer.class.getSimpleName() + "!" + ScheduleSingletonOneTimer.class.getName());
        waitUntilThreadPoolProcessedAtLeast(2, 10000);
        Assert.assertTrue("There must be at least 2 different threads as at least 2 tasks were processed and" +
                " in different threads", scheduleSingletonLocal.getThreadNames().size() > 1);
    }

    public static class ServerSetup extends SnapshotRestoreSetupTask {
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            // create one
            ModelNode writeMaxThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads", MAX_THREADS);
            ModelNode writeCoreThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads", CORE_THREADS);
            ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                    new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", KEEP_ALIVE_TIME_UNIT)
                            .add("time", KEEEP_ALIVE_TIME)));
            ManagementOperations.executeOperation(client.getControllerClient(), writeMaxThreadsOp);
            ManagementOperations.executeOperation(client.getControllerClient(), writeCoreThreadsOp);
            ManagementOperations.executeOperation(client.getControllerClient(), writeKeepAliveTimeOp);
        }
    }
}


