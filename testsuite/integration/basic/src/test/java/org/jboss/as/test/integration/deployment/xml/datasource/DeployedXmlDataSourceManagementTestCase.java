/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.xml.datasource;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

/**
 * Test deployment of -ds.xml files
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DeployedXmlDataSourceManagementTestCase.DeployedXmlDataSourceManagementTestCaseSetup.class)
public class DeployedXmlDataSourceManagementTestCase {

    public static final String TEST_DS_XML = "test-ds.xml";

    static class DeployedXmlDataSourceManagementTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final String packageName = DeployedXmlDataSourceManagementTestCase.class.getPackage().getName().replace(".", "/");
            final DeploymentPlan plan = manager.newDeploymentPlan().add(DeployedXmlDataSourceManagementTestCase.class.getResource("/" + packageName + "/" + TEST_DS_XML)).andDeploy().build();
            final Future<ServerDeploymentPlanResult> future = manager.execute(plan);
            final ServerDeploymentPlanResult result = future.get(20, TimeUnit.SECONDS);
            final ServerDeploymentActionResult actionResult = result.getDeploymentActionResult(plan.getId());
            if (actionResult != null) {
                if (actionResult.getDeploymentException() != null) {
                    throw new RuntimeException(actionResult.getDeploymentException());
                }
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(managementClient.getControllerClient());
            final DeploymentPlan undeployPlan = manager.newDeploymentPlan().undeploy(TEST_DS_XML).andRemoveUndeployed().build();
            manager.execute(undeployPlan);
        }
    }

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testDsXmlDeployment.jar")
                .addClass(DeployedXmlDataSourceManagementTestCase.class)
                .addAsManifestResource(DeployedXmlDataSourceManagementTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
    }

    @Test
    public void testDeployedDatasourceInManagementModel() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("deployment", TEST_DS_XML);
        address.add("subsystem", "datasources");
        address.add("data-source", "java:jboss/datasources/DeployedDS");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("connection-url");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", result.get(RESULT).asString());
    }

    @Test
    public void testDeployedXaDatasourceInManagementModel() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("deployment", TEST_DS_XML);
        address.add("subsystem", "datasources");
        address.add("xa-data-source", "java:/H2XADS");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("driver-name");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals("h2", result.get(RESULT).asString());
    }

    @Test
    public void testDeployedXaDatasourcePropertiesInManagementModel() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("deployment", TEST_DS_XML);
        address.add("subsystem", "datasources");
        address.add("xa-data-source", "java:/H2XADS");
        address.add("xa-datasource-properties", "URL");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("read-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(VALUE);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals("jdbc:h2:mem:test", result.get(RESULT).asString());
    }
}
