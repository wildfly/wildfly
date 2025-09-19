/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.wildfly.test.integration.microprofile.reactive.KeystoreUtil.SERVER_KEYSTORE_PATH;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.dmr.ModelNode;
import org.junit.AssumptionViolatedException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Setup task to start an embedded version of Artemis for AMQP support.
 * I don't want to use the subsystem one because it means needing to run standalone-full.xml. Also, product does not
 * include the AMQP protocol, and would need the protocol module added which in turn would mean adjustments
 * of other module.xml files.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@TestcontainersRequired
public class RunArtemisAmqpSetupTask implements ServerSetupTask {
    private static GenericContainer<?> container;
    private volatile boolean copyKeystore = false;

    private volatile String brokerXml = "messaging/amqp/broker.xml";

    private static final int AMQP_PORT = 5672;
    private static final PathElement AMQP_PORT_PATH = PathElement.pathElement(SYSTEM_PROPERTY, "calculated.amqp.port");

    public RunArtemisAmqpSetupTask() {
    }

    public RunArtemisAmqpSetupTask(String brokerXml, boolean copyKeystore) {
        this.brokerXml = brokerXml;
        this.copyKeystore = copyKeystore;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        try {
            DockerImageName imageName = DockerImageName.parse("quay.io/arkmq-org/activemq-artemis-broker:artemis.2.42.0");
            container = new GenericContainer<>(imageName);
            container.addExposedPort(AMQP_PORT);
            container.withEnv(Map.of(
                    "AMQ_ROLE", "amq",
                    "AMQ_USER", "artemis",
                    "AMQ_PASSWORD", "artemis",
                    "SCRIPT_DEBUG", "true"));
            // Grant all possible file permissions since it ends up under the wrong user, and I can't find how to change
            // the owner/group with testcontainers
            Path path = Paths.get(RunArtemisAmqpSetupTask.class.getResource("messaging/amqp/launch.sh").toURI());
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(path, 0777),
                    "/opt/amq/bin/launch.sh"
            );
            path = Paths.get(RunArtemisAmqpSetupTask.class.getResource(brokerXml).toURI());
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(path),
                    "/home/jboss/config/broker.xml"
            );
            if (copyKeystore) {
                KeystoreUtil.createKeystores();
                // Copy the keystore files to the expected container location
                // The subclass should have configured the keystore in the broker.xml
                container.withCopyFileToContainer(
                    MountableFile.forHostPath(SERVER_KEYSTORE_PATH.getParent()),
                    "/home/jboss/config/");


            }

            try {
                container.start();
            } catch (Exception e) {
                // Either throw AssumptionViolatedException because we are ignoring intermittent failures,
                // or propagate the exception and fail
                IntermittentFailure.thisTestIsFailingIntermittently("https://issues.redhat.com/browse/WFLY-20945");
                throw e;
            }

            // Set the calculated port as a property in the model
            int amqpPort = container.getMappedPort(AMQP_PORT);
            ModelNode op = Util.createAddOperation(PathAddress.pathAddress(AMQP_PORT_PATH), Map.of(VALUE, new ModelNode(amqpPort)));
            ModelNode result =  managementClient.getControllerClient().execute(op);
            ModelTestUtils.checkOutcome(result);
        } catch (Exception e) {
            try {
                tearDown(managementClient, containerId);
            } catch (Exception ex) {
                e.printStackTrace();
            }
            if (e instanceof AssumptionViolatedException ave) {
                throw ave;
            }
            throw new RuntimeException(e);
        }
    }


    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        try {
            ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(AMQP_PORT_PATH));
            managementClient.getControllerClient().execute(op);

            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (copyKeystore) {
                KeystoreUtil.cleanUp();
            }
        }
    }
}
