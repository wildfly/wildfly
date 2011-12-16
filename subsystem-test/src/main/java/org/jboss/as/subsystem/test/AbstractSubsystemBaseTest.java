/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.subsystem.test;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * A test routine every subsystem should go through.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractSubsystemBaseTest extends AbstractSubsystemTest {

    public AbstractSubsystemBaseTest(final String mainSubsystemName, final Extension mainExtension) {
        super(mainSubsystemName, mainExtension);
    }

    /**
     * Get the subsystem xml as string.
     *
     * @return the subsystem xml
     * @throws IOException
     */
    protected abstract String getSubsystemXml() throws IOException;


    /**
     * Get the subsystem xml with the given id as a string.
     * <p>
     * This default implementation returns the result of a call to {@link #readResource(String)}.
     * </p>
     *
     * @param configId the id of the xml configuration
     *
     * @return the subsystem xml
     * @throws IOException
     */
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    /**
     * Validate the marshalled xml.
     *
     * @param original the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @throws Exception
     */
    protected void validateXml(final String original, final String marshalled) throws Exception {
        // TODO check if the marshalled xml can be validated against the schema
    }

    @Test
    public void testSubsystem() throws Exception {
        standardSubsystemTest(null);
    }

    /**
     * Tests the ability to create a model from an xml configuration, marshal the model back to xml,
     * re-read that marshalled model into a new model that matches the first one, execute a "describe"
     * operation for the model, create yet another model from executing the results of that describe
     * operation, and compare that model to first model.
     *
     * @param configId  id to pass to {@link #getSubsystemXml(String)} to get the configuration; if {@code null}
     *                  {@link #getSubsystemXml()} will be called
     *
     * @throws Exception
     */
    protected void standardSubsystemTest(final String configId) throws Exception {
        final AdditionalInitialization additionalInit = createAdditionalInitialization();

        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = configId == null ? getSubsystemXml() : getSubsystemXml(configId);
        final KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        Assert.assertNotNull(servicesA);
        //Get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        Assert.assertNotNull(modelA);

        // Test marshaling
        final String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();


        // validate the the normalized xmls
        String normalizedSubsystem = normalizeXML(subsystemXml);
        validateXml(normalizedSubsystem, normalizeXML(marshalled));

        //Install the persisted xml from the first controller into a second controller
        final KernelServices servicesB = super.installInController(additionalInit, marshalled);
        final ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

        // Test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesB.executeOperation(operation);
        Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem",
                !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
        servicesB.shutdown();

        final KernelServices servicesC = super.installInController(additionalInit, operations);
        final ModelNode modelC = servicesC.readWholeModel();

        super.compare(modelA, modelC);

        super.assertRemoveSubsystemResources(servicesA);
    }

    protected ModelNode createDescribeOperation() {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, getMainSubsystemName());

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DESCRIBE);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return operation;
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }
        };
    }
}
