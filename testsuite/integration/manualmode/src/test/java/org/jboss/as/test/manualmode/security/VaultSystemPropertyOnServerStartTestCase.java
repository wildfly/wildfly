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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.security.common.BasicVaultServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrintSystemPropertyServlet;
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

/**
 * Test whether server starts when system property contain vault value.
 *
 * @author olukas
 *
 */
@RunWith(Arquillian.class)
public class VaultSystemPropertyOnServerStartTestCase {

    private static Logger LOGGER = Logger.getLogger(VaultSystemPropertyOnServerStartTestCase.class);

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "test-deployment";

    private ManagementClient managementClient;
    private BasicVaultServerSetupTask serverSetup = new BasicVaultServerSetupTask();

    public static final String TESTING_SYSTEM_PROPERTY = "vault.testing.property";
    public static final PathAddress SYSTEM_PROPERTIES_PATH = PathAddress.pathAddress().append(SYSTEM_PROPERTY,
            TESTING_SYSTEM_PROPERTY);
    private static final String printPropertyServlet = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/"
            + DEPLOYMENT + PrintSystemPropertyServlet.SERVLET_PATH + "?" + PrintSystemPropertyServlet.PARAM_PROPERTY_NAME + "="
            + TESTING_SYSTEM_PROPERTY;

    @Test
    public void testVaultedSystemPropertyOnStart() throws Exception {
        LOGGER.trace("*** starting server");
        container.start(CONTAINER);

        deployer.deploy(DEPLOYMENT);

        LOGGER.trace("Try to access " + printPropertyServlet);
        String response = HttpRequest.get(printPropertyServlet, 10, TimeUnit.SECONDS);
        Assert.assertTrue("Vaulted system property wasn't read successfully",
                response.contains(BasicVaultServerSetupTask.VAULT_ATTRIBUTE));

        deployer.undeploy(DEPLOYMENT);

    }

    @Before
    public void beforeTest() throws Exception {

        LOGGER.trace("*** starting server");
        container.start(CONTAINER);

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "remote+http");

        serverSetup.setup(managementClient, CONTAINER);

        LOGGER.trace("Add system property: " + TESTING_SYSTEM_PROPERTY);
        ModelNode op = Util.createAddOperation(SYSTEM_PROPERTIES_PATH);
        op.get(VALUE).set(BasicVaultServerSetupTask.VAULTED_PROPERTY);
        Utils.applyUpdate(op, managementClient.getControllerClient());

        LOGGER.trace("*** stoping server");
        container.stop(CONTAINER);
        Thread.sleep(1000);
        int i = 0;
        while (managementClient.isServerInRunningState() && ++i < 200) {
            Thread.sleep(50);
        }

    }

    @After
    public void afterTest() throws Exception {

        LOGGER.trace("Remove system property: " + TESTING_SYSTEM_PROPERTY);
        ModelNode op = Util.createRemoveOperation(SYSTEM_PROPERTIES_PATH);
        Utils.applyUpdate(op, managementClient.getControllerClient());

        serverSetup.tearDown(managementClient, CONTAINER);

        LOGGER.trace("*** stoping server");
        container.stop(CONTAINER);
        Thread.sleep(1000);
        int i = 0;
        while (managementClient.isServerInRunningState() && ++i < 200) {
            Thread.sleep(50);
        }
    }

    @Deployment(name = DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClass(PrintSystemPropertyServlet.class);
        war.addAsManifestResource(VaultSystemPropertyOnServerStartTestCase.class.getPackage(),
                VaultSystemPropertyOnServerStartTestCase.class.getSimpleName() + "-permissions.xml", "permissions.xml");
        return war;
    }

}
