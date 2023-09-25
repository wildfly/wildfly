/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BadResourceTestCase {

    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    public static Archive<?> getTestedArchive() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.TESTED_ARCHIVE_NAME);
        jar.addClasses(SampleEJBImpl.class, ResourceEJBImpl.class);
        jar.addClasses(Constants.class, SampleEJB.class, ResourceEJB.class);

        return jar;
    }

    @Before
    public void createDeployment() throws Exception {
        final ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT,
                Constants.TESTED_DU_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);

        final OperationBuilder ob = new OperationBuilder(addDeploymentOp, true);
        ob.addInputStream(getTestedArchive().as(ZipExporter.class).exportAsInputStream());
        final ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @After
    public void removeDeployment() throws Exception {
        final ModelNode remove = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE,
                new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, Constants.TESTED_DU_NAME));
        final OperationBuilder ob = new OperationBuilder(remove, true);
        final ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testBadDU() throws Exception {

        final ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, Constants.TESTED_DU_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        final OperationBuilder ob = new OperationBuilder(deployOp, true);
        final ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, !Operations.isSuccessfulOutcome(result));

        // asserts
        String failureDescription = result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).toString();
        MatcherAssert.assertThat(String.format("Results doesn't contain correct error code (%s): %s", Constants.ERROR_MESSAGE, result.toString()),
                failureDescription, containsString(Constants.ERROR_MESSAGE));
        MatcherAssert.assertThat(String.format("Results doesn't contain correct JNDI in error message (%s): %s", Constants.JNDI_NAME_BAD, result.toString()),
                failureDescription, containsString(Constants.JNDI_NAME_BAD));
    }

}
