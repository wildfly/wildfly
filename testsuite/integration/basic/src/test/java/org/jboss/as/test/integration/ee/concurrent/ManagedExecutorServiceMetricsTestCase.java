/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.concurrent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.subsystem.EESubsystemModel;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceMetricsAttributes;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import java.io.FilePermission;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test case for managed executor runtime stats
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class ManagedExecutorServiceMetricsTestCase {

    private static final Logger logger = Logger.getLogger(ManagedExecutorServiceMetricsTestCase.class);

    private static final PathAddress EE_SUBSYSTEM_PATH_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EeExtension.SUBSYSTEM_NAME));
    private static final String RESOURCE_NAME = ManagedExecutorServiceMetricsTestCase.class.getSimpleName();

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, ManagedExecutorServiceMetricsTestCase.class.getSimpleName() + ".jar")
                .addClasses(ManagedExecutorServiceMetricsTestCase.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.as.ee, org.jboss.remoting3\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    @Test
    public void testManagedExecutorServiceManagement() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/executor/" + RESOURCE_NAME;
        addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        // note: threads will increase till CORE_THREADS config value, then reuses if has idle thread
        addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(2);
        // task will be considered hung if duration > 1s
        addOperation.get(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(1000);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testRuntimeStats(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }


    @Test
    public void testManagedScheduledExecutorServiceManagement() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/scheduledexecutor/" + RESOURCE_NAME;
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        // note: threads will increase till CORE_THREADS config value, then reuses if has idle thread
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(2);
        // task will be considered hung if duration > 1s
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(1000);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            // lookup the executor
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testRuntimeStats(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

    private void testRuntimeStats(PathAddress pathAddress, ManagedExecutorService executorService) throws IOException, ExecutionException, InterruptedException, BrokenBarrierException {

        // assert stats initial values
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.ACTIVE_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.COMPLETED_TASK_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.MAX_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.TASK_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.THREAD_COUNT) == 0);

        // execute task #1 and assert stats values
        executorService.submit(() -> {
            logger.info("Executing task #1");
            try {
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.ACTIVE_THREAD_COUNT) == 1);
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.COMPLETED_TASK_COUNT) == 0);
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 0);
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.MAX_THREAD_COUNT) == 1);
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.TASK_COUNT) == 1);
                Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.THREAD_COUNT) == 1);
            } catch (IOException e) {
                Assert.fail();
            }
        }).get();

        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.ACTIVE_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.COMPLETED_TASK_COUNT) == 1);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.MAX_THREAD_COUNT) == 1);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.TASK_COUNT) == 1);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.THREAD_COUNT) == 1);

        // execute task #2 and assert stats values
        executorService.submit(() -> {
            logger.info("Executing task #2");
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // after sleeping for over hung threshold time the thread executing this should be considered hung...
            try {
                Assert.assertTrue("Hung thread count should be 1", readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT) == 1);
            } catch (IOException e) {
                Assert.fail();
            }
        }).get();
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.ACTIVE_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.COMPLETED_TASK_COUNT) == 2);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.MAX_THREAD_COUNT) == 2);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.TASK_COUNT) == 2);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.THREAD_COUNT) == 2);

        // exclusive testing of queue size stat
        final CyclicBarrier barrier1 = new CyclicBarrier(3);
        final CyclicBarrier barrier2 = new CyclicBarrier(3);
        final Future f1 = executorService.submit(() -> {
            logger.info("Executing task #3.1");
            try {
                barrier1.await();
                barrier2.await();
            } catch (Exception e) {
                Assert.fail();
            }
        });
        final Future f2 = executorService.submit(() -> {
            logger.info("Executing task #3.2");
            try {
                barrier1.await();
                barrier2.await();
            } catch (Exception e) {
                Assert.fail();
            }
        });
        barrier1.await();
        // 2 tasks running, executing a 3rd should place it in queue,
        // cause when core threads is reached executor always prefers queueing than creating a new thread
        final Future f3 = executorService.submit(() -> {
            logger.info("Executing task #3.3");
        });
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 1);
        // resume tasks running, ensure all complete and then confirm expected idle stats
        barrier2.await();
        f1.get();
        f2.get();
        f3.get();
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.ACTIVE_THREAD_COUNT) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.COMPLETED_TASK_COUNT) == 5);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.CURRENT_QUEUE_SIZE) == 0);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.MAX_THREAD_COUNT) == 2);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.TASK_COUNT) == 5);
        Assert.assertTrue(readStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.THREAD_COUNT) == 2);
    }

    private int readStatsAttribute(PathAddress resourceAddress, String attrName) throws IOException {
        ModelNode op = Util.getReadAttributeOperation(resourceAddress, attrName);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        return result.get(RESULT).asInt();
    }
}
