/*
* JBoss, Home of Professional Open Source.
* Copyright 2014, Red Hat, Inc. and individual contributors
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
package org.jboss.as.test.integration.management.deploy.runtime;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.deploy.runtime.servlet.Servlet;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(Arquillian.class)
@RunAsClient
public class ServletRuntimeNameTestCase extends AbstractRuntimeTestCase {

    private static final Logger log = Logger.getLogger(ServletRuntimeNameTestCase.class);
    private static final Class SERVLET_CLASS = Servlet.class;

    private static final String RT_MODULE_NAME = "nooma-nooma7";
    private static final String RT_NAME = RT_MODULE_NAME + ".ear";
    private static final String DEPLOYMENT_MODULE_NAME = "test7-test";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_MODULE_NAME + ".ear";
    private static final String SUB_DEPLOYMENT_MODULE_NAME = "servlet";
    private static final String SUB_DEPLOYMENT_NAME = SUB_DEPLOYMENT_MODULE_NAME + ".war";
    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SUB_DEPLOYMENT_NAME);
        war.addClass(SERVLET_CLASS);

        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME);
        earArchive.addAsModule(war);

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        addDeploymentOp.get(ModelDescriptionConstants.RUNTIME_NAME).set(RT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.AUTO_START).set(true);
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(earArchive.as(ZipExporter.class).exportAsInputStream());

        ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ModelNode result = controllerClient.execute(composite(
                undeploy(DEPLOYMENT_NAME),
                remove(DEPLOYMENT_NAME)
        ));
        // just to blow up
        Assert.assertTrue("Failed to undeploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testStepByStep() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBDEPLOYMENT, SUB_DEPLOYMENT_NAME);
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add("servlet", SERVLET_CLASS.getName());
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testRecursive() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testServletCall() throws Exception {
        String url = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/"+SUB_DEPLOYMENT_MODULE_NAME+Servlet.URL_PATTERN;
        String res = HttpRequest.get(url, 2, TimeUnit.SECONDS);
        Assert.assertEquals(Servlet.SUCCESS, res);
    }

    /** WFLY-2061 test */
    @Test
    public void testDeploymentStatus() throws IOException {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME));
        final ModelNode operation = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        operation.get(NAME).set("status");

        final ModelNode outcome = controllerClient.execute(operation);
        Assert.assertEquals(SUCCESS, outcome.get(OUTCOME).asString());
        Assert.assertEquals("OK", outcome.get(RESULT).asString());
    }


}
