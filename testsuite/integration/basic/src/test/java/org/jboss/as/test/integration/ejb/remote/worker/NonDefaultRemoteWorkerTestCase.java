/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.worker;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LEVEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WARNING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WARNINGS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.test.integration.ejb.jndi.Echo;
import org.jboss.as.test.integration.ejb.jndi.EchoBean;
import org.jboss.as.test.integration.ejb.jndi.RemoteEcho;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simple test case to check if we get proper feedback on write op to listener->worker
 * @author baranowb
 *
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class NonDefaultRemoteWorkerTestCase {

    @ArquillianResource
    protected ManagementClient managementClient;

    private static final String NAME_DEPLOYMENT = "echo-ejb-candy.jar"; // module
    private static final String NAME_WORKER = "puppeteer";
    private static final PathAddress ADDRESS_WORKER = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "io"),
            PathElement.pathElement("worker", NAME_WORKER));
    private static final PathAddress ADDRESS_HTTP_LISTENER = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, "undertow"), PathElement.pathElement(SERVER, "default-server"),
            PathElement.pathElement("http-listener", "default"));
    private static final String BAD_LEVEL = "X_X";

    private AutoCloseable serverSnapshot;

    @BeforeEach
    public void before() throws Exception {
        serverSnapshot = ServerSnapshot.takeSnapshot(managementClient);
        addWorker();
    }

    @AfterEach
    public void after() throws Exception {
        serverSnapshot.close();
    }

    @Test
    public void testMe() throws Exception {

        ModelNode result = setHttpListenerWorkerTo(NAME_WORKER,BAD_LEVEL);
        Assertions.assertTrue(Operations.isSuccessfulOutcome(result), result.toString());
        Assertions.assertTrue(result.hasDefined(RESPONSE_HEADERS));
        ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
        Assertions.assertTrue(responseHeaders.hasDefined(WARNINGS));
        List<ModelNode> warnings = responseHeaders.get(WARNINGS).asList();
        Assertions.assertTrue(warnings.size() == 2);
        ModelNode warningLoggerLevel = warnings.get(0);
        String message = warningLoggerLevel.get(WARNING).asString();
        Assertions.assertEquals(ControllerLogger.ROOT_LOGGER.couldntConvertWarningLevel(BAD_LEVEL), message);
        Level level = Level.parse(warningLoggerLevel.get(LEVEL).asString());
        Assertions.assertEquals(Level.ALL,level);
        ModelNode warningWorker = warnings.get(1);
        message = warningWorker.get(WARNING).asString();
        Assertions.assertTrue(message.startsWith("WFLYUT0097:"), String.format("Expected message to start with WFLYUT0097: found %s", message));
        level = Level.parse(warningWorker.get(LEVEL).asString());
        Assertions.assertEquals(Level.WARNING,level);
        //default level is "WARNING, set to severe and check if there are warnings
        result = setHttpListenerWorkerTo("default","SEVERE");
        responseHeaders = result.get(RESPONSE_HEADERS);
        Assertions.assertFalse(responseHeaders.hasDefined(WARNINGS));
    }

    @Deployment(name = NAME_DEPLOYMENT)
    public static JavaArchive create() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, NAME_DEPLOYMENT);
        archive.addClass(EchoBean.class);
        archive.addClass(RemoteEcho.class);
        archive.addClass(Echo.class);

        return archive;
    }

    private ModelNode setHttpListenerWorkerTo(final String name, final String level) throws IOException {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ADDRESS_HTTP_LISTENER.toModelNode());
        op.get(ModelDescriptionConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("worker");
        op.get(VALUE).set(name);
        op.get(OPERATION_HEADERS).get(ModelDescriptionConstants.WARNING_LEVEL).set(level);
        final ModelControllerClient client = managementClient.getControllerClient();
        ModelNode result = client.execute(op);
        return result;
    }

    private void addWorker() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ADDRESS_WORKER.toModelNode());
        op.get(ModelDescriptionConstants.OP).set(ADD);
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assertions.assertTrue(Operations.isSuccessfulOutcome(result), result.toString());
    }
}
