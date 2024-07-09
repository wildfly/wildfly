/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.mvc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp25.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
//@ServerSetup(MVCKrazoSmokeTestCase.Setup.class) TODO migrate this to testsuite/integration/basic and let WFARQ call the ServerSetupTask
public class MVCKrazoSmokeTestCase {

    private static final Logger log = Logger.getLogger(MVCKrazoSmokeTestCase.class.getName());

    // TODO migrate this to testsuite/integration/basic and drop the ContainerController aspect used in manualmode
    private static final String CONTAINER = "stability-preview";
    // TODO migrate this to testsuite/integration/basic and let WFARQ call the ServerSetupTask via @ServerSetup
    private static final ServerSetupTask SETUP_TASK = new MVCKrazoSmokeTestCase.Setup();

    private static final String DEPLOYMENT_NAME = "MVCKrazoSmokeTestCase";

    private static final String MVC_EXTENSION = "org.wildfly.extension.mvc-krazo";
    private static final String MVC_SUBSYSTEM = "mvc-krazo";

    @Deployment(name = DEPLOYMENT_NAME, testable = false, managed = false) // TODO migrate this to testsuite/integration/basic and change to managed = true
    @TargetsContainer(CONTAINER)
    public static WebArchive createDeployment() {
        Descriptors.create(WebAppDescriptor.class);
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addClass(MVCKrazoApplication.class)
                .addClass(MVCKrazoSmokeController.class)
                .addClass(CdiBean.class)
                .addAsWebInfResource(new StringAsset("<html>" +
                                "<p>First Name = [${cdiBean.firstName}]</p>\n" +
                                "<p>Last Name = [${lastName}]</p>\n" +
                                "</html>"),
                        "views/view.jsp");
    }
    // TODO migrate this to testsuite/integration/basic and drop the ContainerController aspect used in manualmode
    @ArquillianResource
    private static ContainerController containerController;

    // TODO migrate this to testsuite/integration/basic and drop the manual deployment aspect used in manualmode
    @ArquillianResource
    private static Deployer deployer;

    // TODO migrate this to testsuite/integration/basic and inject the http listener URL
//    @ArquillianResource
//    private URL url;


    // TODO migrate this to testsuite/integration/basic and drop this
    @Test
    @InSequence(-1)
    public void startAndSetupContainer() throws Exception {

        log.trace("*** starting server");
        containerController.start(CONTAINER);
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "remote+http");

        log.trace("*** will configure server now");
        SETUP_TASK.setup(managementClient, CONTAINER);

        deployer.deploy(DEPLOYMENT_NAME);
    }

    @Test
    @InSequence(1)
    public void test() throws Exception {
        // TODO migrate this to testsuite/integration/basic and inject the http listener URL
        //URL url = new URL(this.url.toExternalForm() + "mvc-smoke/test");
        URL url = new URL("http", TestSuiteEnvironment.getServerAddress(), 8080, "/" + DEPLOYMENT_NAME + "/mvc-smoke/test");
        String s = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
        assertTrue(s, s.contains("Joan"));
        assertTrue(s, s.contains("Jett"));
    }

    // TODO migrate this to testsuite/integration/basic and drop this
    @Test
    @InSequence(10)
    public void stopContainer() throws Exception {
        deployer.undeploy(DEPLOYMENT_NAME);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "remote+http");
        log.trace("*** resetting test configuration");
        SETUP_TASK.tearDown(managementClient, CONTAINER);

        log.trace("*** stopping container");
        containerController.stop(CONTAINER);
    }

    public static class Setup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) {

            if (!Boolean.getBoolean("ts.layers") && !Boolean.getBoolean("ts.bootable.preview")) {
                ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
                ModelNode steps = op.get(STEPS);
                steps.add(Util.createAddOperation(PathAddress.pathAddress(EXTENSION, MVC_EXTENSION)));
                steps.add(Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, MVC_SUBSYSTEM)));
                try {
                    ModelNode response = managementClient.getControllerClient().execute(op);
                    if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
                        throw new RuntimeException(String.format("%s response -- %s", op, response));
                    }
                    ServerReload.executeReloadAndWaitForCompletion(managementClient);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            if (!Boolean.getBoolean("ts.layers")) {
                ModelNode op = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
                ModelNode steps = op.get(STEPS);
                steps.add(Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, MVC_SUBSYSTEM)));
                steps.add(Util.createRemoveOperation(PathAddress.pathAddress(EXTENSION, MVC_EXTENSION)));
                managementClient.getControllerClient().execute(op);

                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            }

        }
    }
}
