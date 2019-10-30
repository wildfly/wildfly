/*
 * Copyright (C) 2016 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.jboss.as.test.integration.deployment;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.EJBBusinessInterface;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.SimpleSLSB;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.EjbInvokingServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.HelloWorldServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests to check that the subdeployments function properly with other subdeployments after using
 * the explode operation.
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2016 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SubDeploymentOperationsTestCase {

    private static final Logger logger = Logger.getLogger(SubDeploymentOperationsTestCase.class);

    private static final String TEST_DEPLOYMENT_NAME = "subdeployment-test.ear";
    private static final String JAR_SUBDEPLOYMENT_NAME = "subdeployment-test-ejb.jar";
    private static final String WAR_SUBDEPLOYMENT_NAME = "subdeployment-test-web.war";
    private static final String ARCHIVED_DEPLOYMENT_ERROR_CODE = "WFLYSRV0258";

    @ContainerResource
    ManagementClient managementClient;

    @Before
    public void setUpDeployment() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, TEST_DEPLOYMENT_NAME);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            undeploy(TEST_DEPLOYMENT_NAME);
            remove(TEST_DEPLOYMENT_NAME);
        }

        result = initialDeploy();
        Assert.assertTrue("Failure to set the initial development up: " + result.toString(),
                Operations.isSuccessfulOutcome(result));

        result = undeploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to undeploy the initial development: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
    }

    @After
    public void cleanUpDeployment() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, TEST_DEPLOYMENT_NAME);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            undeploy(TEST_DEPLOYMENT_NAME);
            remove(TEST_DEPLOYMENT_NAME);
        }
    }

    @Test
    public void testExplodeJarSubDeployment() throws Exception {
        ModelNode result = explode(TEST_DEPLOYMENT_NAME, "");
        Assert.assertTrue("Failure to explode the initial deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = explode(TEST_DEPLOYMENT_NAME, JAR_SUBDEPLOYMENT_NAME);
        Assert.assertTrue("Failure to explode JAR subdeployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = deploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to redeploy the deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        testEjbClassAvailableInServlet();
    }

    @Test
    public void testExplodeWarSubDeployment() throws Exception {
        ModelNode result = explode(TEST_DEPLOYMENT_NAME, "");
        Assert.assertTrue("Failure to explode the initial deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = explode(TEST_DEPLOYMENT_NAME, WAR_SUBDEPLOYMENT_NAME);
        Assert.assertTrue("Failure to explode WAR subdeployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = deploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to redeploy the deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        testEjbClassAvailableInServlet();
    }

    @Test
    public void testExplodeJarAndWarSubDeployment() throws Exception {
        ModelNode result = explode(TEST_DEPLOYMENT_NAME, "");
        Assert.assertTrue("Failure to explode the initial deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = explode(TEST_DEPLOYMENT_NAME, JAR_SUBDEPLOYMENT_NAME);
        Assert.assertTrue("Failure to explode JAR subdeployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = explode(TEST_DEPLOYMENT_NAME, WAR_SUBDEPLOYMENT_NAME);
        Assert.assertTrue("Failure to explode WAR subdeployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        result = deploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to redeploy the deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        testEjbClassAvailableInServlet();
    }

    @Test
    public void testExplodeJarSubDeploymentArchiveDeployment() throws Exception {
        ModelNode result = explode(TEST_DEPLOYMENT_NAME, JAR_SUBDEPLOYMENT_NAME);
        Assert.assertFalse("Exploding JAR subdeployment of archived deployment should fail, but outcome was " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        String failure = Operations.getFailureDescription(result).asString();
        Assert.assertTrue("Exploding JAR subdeployment of archived deployment failed with wrong reason: " + failure,
                failure.contains(ARCHIVED_DEPLOYMENT_ERROR_CODE));
        result = deploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to redeploy the deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        testEjbClassAvailableInServlet();
    }

    @Test
    public void testExplodeWarSubDeploymentArchiveDeployment() throws Exception {
        ModelNode result = explode(TEST_DEPLOYMENT_NAME, WAR_SUBDEPLOYMENT_NAME);
        Assert.assertFalse("Exploding WAR subdeployment of archived deployment should fail, but outcome was " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        String failure = Operations.getFailureDescription(result).asString();
        Assert.assertTrue("Exploding WAR subdeployment of archived deployment failed with wrong reason: " + failure,
                failure.contains(ARCHIVED_DEPLOYMENT_ERROR_CODE));
        result = deploy(TEST_DEPLOYMENT_NAME);
        Assert.assertTrue("Failure to redeploy the deployment: " + result.toString(),
                Operations.isSuccessfulOutcome(result));
        testEjbClassAvailableInServlet();
    }

    private ModelNode initialDeploy() throws Exception {
        ModelNode result;
        List<InputStream> attachments = new ArrayList<>();

        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, JAR_SUBDEPLOYMENT_NAME)
                .addClasses(EJBBusinessInterface.class, SimpleSLSB.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_SUBDEPLOYMENT_NAME)
                .addClasses(HelloWorldServlet.class, EjbInvokingServlet.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, TEST_DEPLOYMENT_NAME)
                .addAsModule(ejbJar)
                .addAsModule(war);

        try (InputStream is = ear.as(ZipExporter.class).exportAsInputStream()) {
            ModelNode compositeOp = new ModelNode();
            ModelNode addOp = new ModelNode();
            addOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            addOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, TEST_DEPLOYMENT_NAME);
            addOp.get(ModelDescriptionConstants.CONTENT).add(ModelDescriptionConstants.INPUT_STREAM_INDEX, 0);
            ModelNode content = new ModelNode();
            content.get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
            ModelNode deployOp = new ModelNode();
            deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
            deployOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, TEST_DEPLOYMENT_NAME);
            compositeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
            compositeOp.get(ModelDescriptionConstants.STEPS).setEmptyList();
            compositeOp.get(ModelDescriptionConstants.STEPS).add(addOp);
            compositeOp.get(ModelDescriptionConstants.STEPS).add(deployOp);

            attachments.add(is);

            result = managementClient.getControllerClient().execute(Operation.Factory.create(compositeOp, attachments));
        }

        return result;
    }

    private ModelNode deploy(String deployment) throws Exception {
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
        deployOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, deployment);
        return managementClient.getControllerClient().execute(deployOp);
    }

    private ModelNode explode(String deployment, String path) throws Exception {
        ModelNode explodeOp = new ModelNode();
        explodeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.EXPLODE);
        explodeOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, deployment);
        if (!path.isEmpty()) {
            explodeOp.get(ModelDescriptionConstants.PATH).set(path);
        }
        return managementClient.getControllerClient().execute(explodeOp);
    }

    private ModelNode undeploy(String deployment) throws Exception {
        ModelNode undeployOp = new ModelNode();
        undeployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEPLOY);
        undeployOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, deployment);
        return managementClient.getControllerClient().execute(undeployOp);
    }

    private ModelNode remove(String deployment) throws Exception {
        ModelNode removeOp = new ModelNode();
        removeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        removeOp.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT, deployment);
        return managementClient.getControllerClient().execute(removeOp);
    }

    private void testEjbClassAvailableInServlet() throws Exception {
        final HttpClient httpClient = HttpClients.createDefault();
        final String message = "JBossEAP";
        final String requestURL = TestSuiteEnvironment.getHttpUrl().toString() + "/subdeployment-test-web" + HelloWorldServlet.URL_PATTERN + "?" + HelloWorldServlet.PARAMETER_NAME + "=" + message;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet at " + requestURL, message, responseMessage);
    }

}
