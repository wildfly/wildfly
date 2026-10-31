/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author baranowb
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class BadResourceTestCase {

    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    public static Archive<?> getTestedArchive() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.TESTED_ARCHIVE_NAME);
        jar.addClasses(SampleEJBImpl.class, ResourceEJBImpl.class);
        jar.addClasses(Constants.class, SampleEJB.class, ResourceEJB.class);

        return jar;
    }

    @BeforeEach
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
        Assertions.assertTrue(Operations.isSuccessfulOutcome(result), "Failed to deploy: " + result);
    }

    @AfterEach
    public void removeDeployment() throws Exception {
        final ModelNode remove = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE,
                new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, Constants.TESTED_DU_NAME));
        final OperationBuilder ob = new OperationBuilder(remove, true);
        final ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assertions.assertTrue(Operations.isSuccessfulOutcome(result), "Failed to deploy: " + result);
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
        Assertions.assertTrue(!Operations.isSuccessfulOutcome(result), "Failed to deploy: " + result);

        // asserts
        String failureDescription = result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).toString();
        assertThat(failureDescription).as(String.format("Results doesn't contain correct error code (%s): %s", Constants.ERROR_MESSAGE, result.toString())).contains(Constants.ERROR_MESSAGE);
        assertThat(failureDescription).as(String.format("Results doesn't contain correct JNDI in error message (%s): %s", Constants.JNDI_NAME_BAD, result.toString())).contains(Constants.JNDI_NAME_BAD);
    }

}
