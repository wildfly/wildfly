/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRIMARY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATUS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TIMEOUT;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createEmptyOperation;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.cleanFile;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;
import static org.junit.Assert.fail;

/**
 * The test case schedules an instance of task ReSchedulingTask.
 * Each instance of ReSchedulingTask sleeps for 10 seconds and then re-schedules another instance of its own class.
 * After the CLI command /host=primary/server-config=server-one:stop() is invoked the server should stop.
 * Test for [ WFCORE-3868 ].
 *
 * @author Daniel Cihak
 */
public class EEConcurrencyExecutorShutdownTestCase {

    private static final String ARCHIVE_FILE_NAME = "test.war";
    private static final PathAddress ROOT_DEPLOYMENT_ADDRESS = PathAddress.pathAddress(DEPLOYMENT, ARCHIVE_FILE_NAME);
    private static final PathAddress MAIN_SERVER_GROUP_ADDRESS = PathAddress.pathAddress(SERVER_GROUP, "main-server-group");
    private static final PathAddress MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS = MAIN_SERVER_GROUP_ADDRESS.append(DEPLOYMENT, ARCHIVE_FILE_NAME);
    public static final String FIRST_SERVER_NAME = "main-one";

    private static DomainTestSupport testSupport;
    public static DomainLifecycleUtil domainPrimaryLifecycleUtil;
    private static File tmpDir;

    private static DomainClient primaryClient;

    @BeforeClass
    public static void setupDomain() {
        testSupport = createAndStartDefaultEESupport(EEConcurrencyExecutorShutdownTestCase.class.getSimpleName());
        domainPrimaryLifecycleUtil = testSupport.getDomainPrimaryLifecycleUtil();
        primaryClient = domainPrimaryLifecycleUtil.getDomainClient();
    }

    private static DomainTestSupport createAndStartDefaultEESupport(final String testName) {
        try {
            final DomainTestSupport.Configuration configuration;
            if (Boolean.getBoolean("wildfly.primary.debug")) {
                configuration = DomainTestSupport.Configuration.createDebugPrimary(testName,
                        "domain-configs/domain-standard-ee.xml", "host-configs/host-primary.xml", null);
            } else if (Boolean.getBoolean("wildfly.secondary.debug")) {
                configuration = DomainTestSupport.Configuration.createDebugSecondary(testName,
                        "domain-configs/domain-standard-ee.xml", "host-configs/host-primary.xml", null);
            } else {
                configuration = DomainTestSupport.Configuration.create(testName,
                        "domain-configs/domain-standard-ee.xml", "host-configs/host-primary.xml", null);
            }
            final DomainTestSupport testSupport = DomainTestSupport.create(configuration);
            testSupport.start();
            return testSupport;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void createDeployment() {
        WebArchive testDeployment = ShrinkWrap.create(WebArchive.class, ARCHIVE_FILE_NAME);
        testDeployment.addClasses(TaskSchedulerServletContextListener.class);
        tmpDir = new File("target/deployments/" + this.getClass().getSimpleName());
        new File(tmpDir, "archives").mkdirs();
        testDeployment.as(ZipExporter.class).exportTo(new File(tmpDir, "archives/" + ARCHIVE_FILE_NAME), true);
    }

    @AfterClass
    public static void tearDownDomain() {
        try {
            testSupport.stop();
            domainPrimaryLifecycleUtil = null;
        } finally {
            cleanFile(tmpDir);
        }
    }

    /**
     * Tests if the server with running ConcurrencyExecutor can be stopped using cli command
     * /host=primary/server-config=server-one:stop(timeout=0)
     *
     * @throws Exception
     */
    @Test
    public void testConcurrencyExecutorShutdown() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + ARCHIVE_FILE_NAME).getAbsolutePath());
        ModelNode deploymentOpMain = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        executeForResult(deploymentOpMain, primaryClient);

        this.stopServer(FIRST_SERVER_NAME);
    }

    private ModelNode createDeploymentOperation(ModelNode content, PathAddress... serverGroupAddressses) {
        ModelNode composite = createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        ModelNode step1 = steps.add();
        step1.set(createAddOperation(ROOT_DEPLOYMENT_ADDRESS));
        step1.get(CONTENT).add(content);
        for (PathAddress serverGroup : serverGroupAddressses) {
            ModelNode sg = steps.add();
            sg.set(createAddOperation(serverGroup));
            sg.get(ENABLED).set(true);
        }
        return composite;
    }

    /**
     * Stops the given server and waits until STOPPED status
     *
     * @param serverName
     * @throws Exception
     */
    private void stopServer(final String serverName) {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(HOST, PRIMARY);
        op.get(OP_ADDR).add(SERVER_CONFIG, FIRST_SERVER_NAME);
        op.get(OP).set(STOP);
        op.get(TIMEOUT).set(0);
        domainPrimaryLifecycleUtil.executeForResult(op);
        try {
            waitUntilState(FIRST_SERVER_NAME, "STOPPED");
        } catch (TimeoutException e) {
            fail("After \"stop(timeout=0)\" was called sever never reached the desired STOPPED state.");
        }
    }

    /**
     * Checks the status of he given server until given state is reached.
     *
     * @param serverName
     * @param state
     * @throws TimeoutException
     */
    private static void waitUntilState(final String serverName, final String state) throws TimeoutException {
        RetryTaskExecutor<Void> taskExecutor = new RetryTaskExecutor<Void>();
        taskExecutor.retryTask(new Callable<Void>() {
            public Void call() throws Exception {
                String serverStatus = this.checkServerStatus();
                if (!serverStatus.equals(state)) throw new Exception("Server not in state " + state);
                return null;
            }

            private String checkServerStatus() {
                ModelNode op = new ModelNode();
                op.get(OP_ADDR).add(HOST, PRIMARY);
                op.get(OP_ADDR).add(SERVER_CONFIG, serverName);
                op.get(OP).set(READ_ATTRIBUTE_OPERATION);
                op.get(NAME).set(STATUS);
                return domainPrimaryLifecycleUtil.executeForResult(op).asString();
            }
        });
    }
}
