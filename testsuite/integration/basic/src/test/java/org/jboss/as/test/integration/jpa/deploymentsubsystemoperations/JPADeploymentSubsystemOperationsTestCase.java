/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.deploymentsubsystemoperations;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * Regression tests for WFLY-11173
 * Tests proper operations behavior in /deployment/subsystem=jpa case
 *
 * @author <a href="mailto:pberan@redhat.com">Petr Beran
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JPADeploymentSubsystemOperationsTestCase {

    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    private static final PathAddress DEPLOYMENT_PATH = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, JPADeploymentSubsystemOperationsTestCase.class.getSimpleName() + ".war"));
    private static final PathAddress SUBSYSTEM_PATH = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "jpa"));

    // Hardcoded to avoid adding the JPA module as a dependency
    // Same goes for the "jpa" in SUBSYSTEM_PATH
    private static final String DEFAULT_DATASOURCE = "default-datasource";
    private static final String DEFAULT_EXTENDED_PERSISTENCE_INHERITANCE = "default-extended-persistence-inheritance";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, JPADeploymentSubsystemOperationsTestCase.class.getSimpleName() + ".war");
        war.addAsWebInfResource(JPADeploymentSubsystemOperationsTestCase.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml");
        return war;
    }

    /**
     * Tests the proper behavior for the {@code read-resource} operation on the JPA subsystem in a deployment.
     * The {@code read-resource} should return the main subsystem values instead of the deployment's subsystem ones.
     *
     * @throws IOException if an error occurs during the {@code ModelNode} execution.
     */
    @Test
    public void testReadResourceOperation() throws IOException {
        // We need to change the subsystem's attribute from a default one. Otherwise we would be checking a default value
        // against a default value and the test would pass every time.
        // The default value for default-extended-persistence-inheritance is DEEP, so we're changing it to SHALLOW, see JPADefinition class
        ModelNode writeAttributeModelNode = Util.getWriteAttributeOperation(SUBSYSTEM_PATH, DEFAULT_EXTENDED_PERSISTENCE_INHERITANCE, "SHALLOW");
        controllerClient.execute(writeAttributeModelNode);

        ModelNode subsystemModelNode = Util.getReadResourceOperation(SUBSYSTEM_PATH);
        ModelNode subsystemResult = controllerClient.execute(subsystemModelNode);

        ModelNode deploymentModelNode = Util.getReadResourceOperation(DEPLOYMENT_PATH.append(SUBSYSTEM_PATH));
        ModelNode deploymentResult = controllerClient.execute(deploymentModelNode);

        Assert.assertEquals("The attribute in the subsystem and in the deployment is not the same",
                subsystemResult.get(RESULT).get(DEFAULT_EXTENDED_PERSISTENCE_INHERITANCE),
                deploymentResult.get(RESULT).get(DEFAULT_EXTENDED_PERSISTENCE_INHERITANCE));
        Assert.assertEquals("The attribute in the subsystem and in the deployment is not the same",
                subsystemResult.get(RESULT).get(DEFAULT_DATASOURCE),
                deploymentResult.get(RESULT).get(DEFAULT_DATASOURCE));
    }

    /**
     * Tests the proper behavior for the {@code add} and {@code remove} operations on the JPA subsystem in a deployment.
     * These operations should fail since they are unsupported in a deployment's subsystem.
     *
     * @throws IOException if an error occurs during the {@code ModelNode} execution.
     */
    @Test
    public void testAddRemoveResource() throws IOException {
        ModelNode removeModelNode = Util.createRemoveOperation(DEPLOYMENT_PATH.append(SUBSYSTEM_PATH));
        ModelNode removeResult = controllerClient.execute(removeModelNode);
        checkForFailure(removeResult, REMOVE.toUpperCase(Locale.ENGLISH), "WFLYCTL0031");

        ModelNode addModelNode = Util.createAddOperation(DEPLOYMENT_PATH.append(SUBSYSTEM_PATH));
        ModelNode addResult = controllerClient.execute(addModelNode);
        checkForFailure(addResult, ADD.toUpperCase(Locale.ENGLISH), "WFLYCTL0031");
    }

    /**
     * Tests the proper behavior for the {@code write-attribute} operation on the JPA subsystem in a deployment.
     * The operation should fail since it is unsupported in a deployment's subsystem.
     *
     * @throws IOException if an error occurs during the {@code ModelNode} execution.
     */
    @Test
    public void testWriteAttribute() throws IOException {
        ModelNode writeAttributeModelNode = Util.getWriteAttributeOperation(DEPLOYMENT_PATH.append(SUBSYSTEM_PATH), DEFAULT_DATASOURCE, "Foobar");
        ModelNode writeAttributeResult = controllerClient.execute(writeAttributeModelNode);
        checkForFailure(writeAttributeResult, WRITE_ATTRIBUTE_OPERATION.toUpperCase(Locale.ENGLISH), "WFLYCTL0048");
    }

    /**
     * Helper method for checking whether the operation failed as expected.
     *
     * @param modelNode The result of the operation in form of {@code ModelNode}
     * @param method The {@code String} representing the operation's name. Used only for the error message purposes.
     * @param errorCode The {@code String} representing expected error code in the operation's result.
     */
    private void checkForFailure(ModelNode modelNode, String method, String errorCode) {
        Assert.assertTrue("The operation '" + method + "' should have failed for the JPA subsystem in a deployment",
                modelNode.get(OUTCOME).asString().equals(FAILED));
        Assert.assertTrue("The operation '" + method + "' failed due to an unexpected reason",
                modelNode.get(FAILURE_DESCRIPTION).asString().contains(errorCode));
    }

}
