/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.security;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * Test whether server starts when system property contain vault value.
 *
 * @author olukas
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaccInitializationTestCase {

    public static final String JACC_POLICY = "jacc-policy";
    public static final String INITIALIZE_JACC = "initialize-jacc";
    public static final String SECURITY = "security";
    public static final String ELYTRON = "elytron";
    public static final String POLICY = "policy";
    private static Logger LOGGER = Logger.getLogger(JaccInitializationTestCase.class);

    @ArquillianResource
    private static volatile ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "test-deployment";
    private String snapshot;
    private ModelControllerClient client;

    @Before
    public void setUp() throws Exception {
        container.start(CONTAINER);
        client = createClient().getControllerClient();
        snapshot = takeSnapshot(client);
    }

    @After
    public void tearDown() throws Exception {
        if (container.isStarted(CONTAINER)) {
            container.stop(CONTAINER);
        }
        restoreSnapshot(snapshot);

        client.close();
    }

    @Test
    public void testUnsetLegacyJaccRequiresRestart() throws Exception {
        ModelNode res = enableLegacyJacc();
        assertOperationRequiresRestart(res);

        restartContainer();

        res = disableLegacyJacc(client);
        assertOperationRequiresRestart(res);
    }

    @Test
    public void testEnableElytronJaccRequiresRestart() throws Exception {
        ModelNode res = disableLegacyJacc(client);
        assertOperationRequiresRestart(res);

        res = enableElytronJacc();
        assertOperationRequiresRestart(res);
    }

    @Test
    public void testEnableElytronWhenLegacyJaccActiveThrowsException() throws Exception {
        ModelNode res = enableElytronJacc();
        assertOperationFailed(res, "WFLYELY01086");
    }

    @Test
    public void testEnableLegayJaccWhenElytronActiveThrowsException() throws Exception {
        disableLegacyJacc(client);
        enableElytronJacc();
        restartContainer();

        ModelNode res = enableLegacyJacc();
        assertOperationFailed(res, "WFLYSEC0105");
    }

    @Test
    public void testSwitchJaccBack() throws Exception {
        ModelNode res = disableLegacyJacc(client);
        assertOperationRequiresRestart(res);

        res = enableElytronJacc();
        assertOperationRequiresRestart(res);

        res = disableElytronJacc();
        assertOperationRequiresRestart(res);

        res = enableLegacyJacc();
        assertOperationRequiresRestart(res);
    }

    private ModelNode enableElytronJacc() throws Exception {
        ModelNode enableElectronJacc = Util.createAddOperation(PathAddress.pathAddress().append(SUBSYSTEM, ELYTRON).append(POLICY, "jacc"));
        enableElectronJacc.get(JACC_POLICY).setEmptyObject();
        return client.execute(enableElectronJacc);
    }

    private ModelNode disableElytronJacc() throws Exception {
        ModelNode disableElectronJacc = Util.createRemoveOperation(PathAddress.pathAddress().append(SUBSYSTEM, ELYTRON).append(POLICY, "jacc"));
        return client.execute(disableElectronJacc);
    }

    private ModelNode disableLegacyJacc(ModelControllerClient client) throws Exception {
        ModelNode disableJacc = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress().append(SUBSYSTEM, SECURITY));
        disableJacc.get(NAME).set(INITIALIZE_JACC);
        disableJacc.get(VALUE).set(false);
        return client.execute(disableJacc);
    }

    private ModelNode enableLegacyJacc() throws Exception {
        ModelNode enableJacc = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.pathAddress().append(SUBSYSTEM, SECURITY));
        enableJacc.get(NAME).set(INITIALIZE_JACC);
        enableJacc.get(VALUE).set(true);
        return client.execute(enableJacc);
    }

    private void assertOperationFailed(ModelNode response, String cause) {
        Assert.assertFalse(Operations.isSuccessfulOutcome(response));
        String desc = Operations.getFailureDescription(response).asString();
        Assert.assertTrue("Failure description should contain [" + cause + "] but got [" + desc + "]", desc.contains(cause));
    }

    private void assertOperationRequiresRestart(ModelNode response) throws Exception {
        if (!Operations.isSuccessfulOutcome(response)) {
            throw new Exception("Operation failed " + Operations.getFailureDescription(response));
        }
        Assert.assertTrue(response.get(RESPONSE_HEADERS).get(OPERATION_REQUIRES_RESTART).isDefined());
    }

    private void restartContainer() {
        container.stop(CONTAINER);
        container.start(CONTAINER);
    }

    private ManagementClient createClient() {
        return new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("take-snapshot");
        ModelNode response = client.execute(operation);
        if (!Operations.isSuccessfulOutcome(response)) {
            throw new Exception("Operation failed " + Operations.getFailureDescription(response));
        }
        return response.get(RESULT).asString();
    }

    private void restoreSnapshot(String snapshot) throws IOException {
        Path snapshotFile = new File(snapshot).toPath();
        Path standaloneConfiguration = snapshotFile.getParent().getParent().getParent().resolve("standalone-ha.xml");
        Files.move(snapshotFile, standaloneConfiguration, StandardCopyOption.REPLACE_EXISTING);
    }

    @Deployment(managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
    }

}
