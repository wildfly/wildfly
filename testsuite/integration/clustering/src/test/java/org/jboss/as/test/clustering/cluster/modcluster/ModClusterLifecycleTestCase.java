/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.modcluster;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.STATUS;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Set;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies mod_cluster context lifecycle integration with Undertow: deploy, undeploy, suspend, and resume.
 *
 * @author Radoslav Husar
 */
@RunAsClient
@ExtendWith(ArquillianExtension.class)
@ServerSetup(ModClusterLifecycleTestCase.ServerSetupTask.class)
public class ModClusterLifecycleTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = ModClusterLifecycleTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";
    private static final long STATUS_REFRESH_TIMEOUT = 30_000;
    private static final int LB_OFFSET = 500;

    @Deployment(name = DEPLOYMENT_1, testable = false, managed = false)
    @TargetsContainer(NODE_1)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.add(new StringAsset("Hello World"), "index.html");
        return war;
    }

    public ModClusterLifecycleTestCase() {
        // Note that we are not starting deployments automatically here – Set.of()
        super(Set.of(LOAD_BALANCER_1, NODE_1), Set.of());
    }

    @Test
    void contextIsRegisteredAfterDeploy() throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            assertContextStatus(ModClusterContextStatus.ENABLED);
        } finally {
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    @Test
    void contextIsRemovedAfterUndeploy() throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        assertContextStatus(ModClusterContextStatus.ENABLED);

        deployer.undeploy(DEPLOYMENT_1);
        assertContextRemoved();
    }

    @Test
    void contextIsStoppedWhenStartedInSuspendedMode() throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            assertContextStatus(ModClusterContextStatus.ENABLED);

            // n.b. startSuspended=true
            ServerReload.executeReloadAndWaitForCompletion(TestSuiteEnvironment.getModelControllerClient(), ServerReload.TIMEOUT, false, true, null, -1, null);

            assertContextStatus(ModClusterContextStatus.STOPPED);
        } finally {
            // n.b. the server is still running suspended - we need to resume it back to normal state for the next test
            resume();
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    @Test
    void contextIsStoppedAfterSuspendAndEnabledAfterResume() throws Exception {
        deployer.deploy(DEPLOYMENT_1);
        try {
            assertContextStatus(ModClusterContextStatus.ENABLED);

            suspend();
            try {
                assertContextStatus(ModClusterContextStatus.STOPPED);
            } finally {
                resume();
            }
            assertContextStatus(ModClusterContextStatus.ENABLED);
        } finally {
            deployer.undeploy(DEPLOYMENT_1);
        }
    }

    private void assertContextStatus(ModClusterContextStatus expectedStatus) throws Exception {
        ModelNode op = createContextReadResourceOp();

        try (ModelControllerClient client = createLBClient()) {
            Awaitility.await("context status to become " + expectedStatus)
                    .atMost(Duration.ofMillis(STATUS_REFRESH_TIMEOUT))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        ModelNode modelNode = client.execute(op);
                        assertEquals(SUCCESS, modelNode.get(OUTCOME).asString());
                        assertEquals(expectedStatus.toString(), modelNode.get(RESULT).get(STATUS).asString(), "Context status on load balancer");
                    });
        }
    }

    private void assertContextRemoved() throws Exception {
        ModelNode op = createContextReadResourceOp();

        try (ModelControllerClient client = createLBClient()) {
            Awaitility.await("context to be removed")
                    .atMost(Duration.ofMillis(STATUS_REFRESH_TIMEOUT))
                    .pollInterval(Duration.ofMillis(100))
                    .until(() -> !SUCCESS.equals(client.execute(op).get(OUTCOME).asString()));
        }
    }

    private static ModelNode createContextReadResourceOp() {
        ModelNode op = createOpNode("subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=mycluster/node=" + NODE_1, READ_RESOURCE_OPERATION);
        op.get(ADDRESS).add("context", "/" + MODULE_NAME);
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    private void suspend() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set("suspend");
        op.get(OP_ADDR).setEmptyList();

        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            ModelNode result = client.execute(op);
            assertEquals(SUCCESS, result.get(OUTCOME).asString(), "Suspend operation failed: " + result);
        }
    }

    private void resume() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set("resume");
        op.get(OP_ADDR).setEmptyList();

        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            ModelNode result = client.execute(op);
            assertEquals(SUCCESS, result.get(OUTCOME).asString(), "Resume operation failed: " + result);
        }
    }

    private static ModelControllerClient createLBClient() {
        return TestSuiteEnvironment.getModelControllerClient(
                null,
                TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort() + LB_OFFSET);
    }

    static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=false)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:add(host=localhost, port=8590)")
                            .add("/subsystem=modcluster/proxy=default:list-add(name=proxies, value=proxy1)")
                            .endBatch()
                            .build())
                    .build()
            );
        }
    }
}
