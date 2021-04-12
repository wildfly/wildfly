/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ee.subsystem.ManagedExecutorTerminateHungTasksOperation;
import org.jboss.dmr.ModelNode;
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Test case for managed executor's hung tasks termination feature.
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class HungTasksTerminationTestCase {

    private static final PathAddress EE_SUBSYSTEM_PATH_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EeExtension.SUBSYSTEM_NAME));
    private static final String RESOURCE_NAME = HungTasksTerminationTestCase.class.getSimpleName();
    // a task is considered hung when is running over 0.5s
    private static final long HUNG_TASK_THRESHOLD_TEST_VALUE = 500;
    // hung tasks periodic cancellation should be done every 5s
    private static final long HUNG_TASK_CANCELLATION_PERIOD_TEST_VALUE = 5000;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, HungTasksTerminationTestCase.class.getSimpleName() + ".jar")
                .addClasses(HungTasksTerminationTestCase.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.as.ee, org.jboss.remoting\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    /**
     * Tests the 'on demand' hung task termination, through a management operation, works on a ManagedExecutorService.
     * @throws Exception
     */
    @Test
    public void testManagedExecutorServiceHungTasksCancellationOperation() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/executor/" + RESOURCE_NAME;
        addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(HUNG_TASK_THRESHOLD_TEST_VALUE);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testHungTasksCancellationOperation(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

    /**
     * Tests the 'on demand' hung task termination, through a management operation, works on a ManagedScheduledExecutorService.
     * @throws Exception
     */
    @Test
    public void testManagedScheduledExecutorServiceHungTasksCancellationOperation() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/scheduledexecutor/" + RESOURCE_NAME;
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(HUNG_TASK_THRESHOLD_TEST_VALUE);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            // lookup the executor
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testHungTasksCancellationOperation(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

    /**
     * Tests the 'periodic' hung task termination works on a ManagedExecutorService.
     * @throws Exception
     */
    @Test
    public void testManagedExecutorServiceHungTasksCancellationPeriodic() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/executor/" + RESOURCE_NAME;
        addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(HUNG_TASK_THRESHOLD_TEST_VALUE);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_TERMINATION_PERIOD).set(HUNG_TASK_CANCELLATION_PERIOD_TEST_VALUE);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testHungTasksCancellationPeriodic(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

    /**
     * Tests the 'periodic' hung task termination works on a ManagedScheduledExecutorService.
     * @throws Exception
     */
    @Test
    public void testManagedScheduledExecutorServiceHungTasksCancellationPeriodic() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        final String jndiName = "java:jboss/ee/concurrency/scheduledexecutor/" + RESOURCE_NAME;
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(HUNG_TASK_THRESHOLD_TEST_VALUE);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_TERMINATION_PERIOD).set(HUNG_TASK_CANCELLATION_PERIOD_TEST_VALUE);
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        try {
            // lookup the executor
            final ManagedExecutorService executorService = InitialContext.doLookup(jndiName);
            Assert.assertNotNull(executorService);
            testHungTasksCancellationPeriodic(pathAddress, executorService);
        } finally {
            // remove
            final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

    private void testHungTasksCancellationOperation(PathAddress pathAddress, ManagedExecutorService executorService) throws IOException, ExecutionException, InterruptedException, BrokenBarrierException {
        // assert no hung tasks exists at start
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 0);
        final CyclicBarrier barrier1 = new CyclicBarrier(2);
        final CyclicBarrier barrier2 = new CyclicBarrier(2);
        final Runnable runnable = () -> {
            try {
                // signal task is running
                barrier1.await();
                // sleep till hung
                Thread.sleep(HUNG_TASK_THRESHOLD_TEST_VALUE + 1);
                // signal task is hung
                barrier2.await();
            } catch (Exception e) {
                // unexpected, thus fail
                Assert.fail();
            }
            // sleep 1 min, expecting cancellation
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
                // expected, was cancelled
                throw new RuntimeException(e);
            }
            // not expected, was not cancelled
            Assert.fail();
        };
        executorService.submit(runnable);
        barrier1.await();
        // all crossed the 1st barrier, task is running
        barrier2.await();
        // all crossed the 2nd barrier, task should be considered hung
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 1);
        // let's invoke the op to cancel the hung tasks
        executeHungTasksCancellationOperation(pathAddress);
        // and assert now no hung tasks exists
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 0);
    }

    private void testHungTasksCancellationPeriodic(PathAddress pathAddress, ManagedExecutorService executorService) throws IOException, ExecutionException, InterruptedException, BrokenBarrierException {
        // assert no hung tasks exists at start
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 0);
        final CyclicBarrier barrier1 = new CyclicBarrier(2);
        final CyclicBarrier barrier2 = new CyclicBarrier(2);
        final Runnable runnable = () -> {
            try {
                // signal task is running
                barrier1.await();
                // sleep till hung
                Thread.sleep(HUNG_TASK_THRESHOLD_TEST_VALUE + 1);
                // signal task is hung
                barrier2.await();
            } catch (Exception e) {
                // unexpected, thus fail
                Assert.fail();
            }
            // sleep 1 min, expecting cancellation
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
                // expected, was cancelled
                throw new RuntimeException(e);
            }
            // not expected, was not cancelled
            Assert.fail();
        };
        executorService.submit(runnable);
        barrier1.await();
        // all crossed the 1st barrier, task is running
        barrier2.await();
        // all crossed the 2nd barrier, task should be considered hung
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 1);
        // sleep periodic cancellation
        try {
            Thread.sleep(HUNG_TASK_CANCELLATION_PERIOD_TEST_VALUE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // assert no hung tasks exists
        assertStatsAttribute(pathAddress, ManagedExecutorServiceMetricsAttributes.HUNG_THREAD_COUNT, 0);
    }

    private int readStatsAttribute(PathAddress resourceAddress, String attrName) throws IOException {
        ModelNode op = Util.getReadAttributeOperation(resourceAddress, attrName);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        return result.get(RESULT).asInt();
    }

    private void assertStatsAttribute(PathAddress resourceAddress, String attrName, int expectedAttrValue) throws IOException, InterruptedException {
        int actualAttrValue = readStatsAttribute(resourceAddress, attrName);
        int retries = 5;
        while (actualAttrValue != expectedAttrValue && retries > 0) {
            Thread.sleep(500);
            actualAttrValue = readStatsAttribute(resourceAddress, attrName);
            retries--;
        }
        Assert.assertEquals(attrName, expectedAttrValue, actualAttrValue);
    }

    private void executeHungTasksCancellationOperation(PathAddress resourceAddress) throws IOException {
        ModelNode op = Util.createOperation(ManagedExecutorTerminateHungTasksOperation.NAME, resourceAddress);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
    }
}
