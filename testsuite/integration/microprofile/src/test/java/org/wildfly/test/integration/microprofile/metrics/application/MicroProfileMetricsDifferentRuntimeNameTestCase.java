/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.metrics.application;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.application.resource.ResourceSimple;

@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsDifferentRuntimeNameTestCase {
    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    private static Deployer deployer;

    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    private static final String DEPLOYMENT_NAME = "MicroProfileMetricsDifferentRuntimeNameTestCase.war";
    private static final String DEPLOYMENT_MGMT_NAME = "MicroProfileMetricsDifferentRuntimeNameTestCase-mgmt.war";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(TestApplication.class)
                .addClass(ResourceSimple.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, DEPLOYMENT_MGMT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(INPUT_STREAM_INDEX).set(0);
        addDeploymentOp.get(ModelDescriptionConstants.AUTO_START).set(true);
        addDeploymentOp.get(ModelDescriptionConstants.RUNTIME_NAME).set(DEPLOYMENT_NAME);
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, DEPLOYMENT_MGMT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);
        compositeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(war.as(ZipExporter.class).exportAsInputStream());

        ModelNode result = controllerClient.execute(ob.build());

        Assert.assertTrue("deploy did not fail: " + result, Operations.isSuccessfulOutcome(result));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ModelNode removeDeploymentOp = new ModelNode();
        removeDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, DEPLOYMENT_MGMT_NAME);
        removeDeploymentOp.get(ModelDescriptionConstants.OP).set(REMOVE);
        ModelNode undeployOp = new ModelNode();
        undeployOp.get(ModelDescriptionConstants.OP).set(UNDEPLOY);
        undeployOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, DEPLOYMENT_MGMT_NAME);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = undeployOp;
        steps[1] = removeDeploymentOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);
        compositeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);

        ModelNode result = controllerClient.execute(ob.build());

        Assert.assertTrue(Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testMetricsStatus() throws Exception {
        final String endpointURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(endpointURL);
            CloseableHttpResponse resp = client.execute(request);
            assertEquals(200, resp.getStatusLine().getStatusCode());
        }
    }
}