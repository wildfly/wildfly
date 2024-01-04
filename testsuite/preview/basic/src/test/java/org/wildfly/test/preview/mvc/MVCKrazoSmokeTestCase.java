/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.mvc;

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
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp25.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MVCKrazoSmokeTestCase.Setup.class)
public class MVCKrazoSmokeTestCase {

    private static final Logger log = Logger.getLogger(MVCKrazoSmokeTestCase.class.getName());

    private static final String MVC_EXTENSION = "org.wildfly.extension.mvc-krazo";
    private static final String MVC_SUBSYSTEM = "mvc-krazo";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        Descriptors.create(WebAppDescriptor.class);
        return ShrinkWrap.create(WebArchive.class, MVCKrazoSmokeTestCase.class.getSimpleName() + ".war")
                .addClass(MVCKrazoApplication.class)
                .addClass(MVCKrazoSmokeController.class)
                .addClass(CdiBean.class)
                .addAsWebInfResource(new StringAsset("<html>" +
                                "<p>First Name = [${cdiBean.firstName}]</p>\n" +
                                "<p>Last Name = [${lastName}]</p>\n" +
                                "</html>"),
                        "views/view.jsp");
    }

    @ArquillianResource
    private URL url;

    @Test
    public void test() throws Exception {
        URL url = new URL(this.url.toExternalForm() + "mvc-smoke/test");
        String s = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
        assertTrue(s, s.contains("Joan"));
        assertTrue(s, s.contains("Jett"));
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
