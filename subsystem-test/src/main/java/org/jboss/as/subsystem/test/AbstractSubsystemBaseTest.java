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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * A test routine every subsystem should go through.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public abstract class AbstractSubsystemBaseTest extends AbstractSubsystemTest {

    public AbstractSubsystemBaseTest(final String mainSubsystemName, final Extension mainExtension) {
        super(mainSubsystemName, mainExtension);
    }

    public AbstractSubsystemBaseTest(final String mainSubsystemName, final Extension mainExtension, final Comparator<PathAddress> removeOrderComparator) {
        super(mainSubsystemName, mainExtension, removeOrderComparator);
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
        standardSubsystemTest(configId, true);
    }

    protected KernelServices standardSubsystemTest(final String configId, boolean compareXml) throws Exception {
        return standardSubsystemTest(configId, null, compareXml);
    }

    protected void standardSubsystemTest(final String configId, final String configIdResolvedModel) throws Exception {
        standardSubsystemTest(configId, configIdResolvedModel, true);
    }


    /**
     * Tests the ability to create a model from an xml configuration, marshal the model back to xml,
     * re-read that marshalled model into a new model that matches the first one, execute a "describe"
     * operation for the model, create yet another model from executing the results of that describe
     * operation, and compare that model to first model.
     * if configIdResolvedModel is not null compare the model from configId and one from configIdResolvedModel
     * after all expression have been reolved.
     *
     * @param configId  id to pass to {@link #getSubsystemXml(String)} to get the configuration; if {@code null}
     *                  {@link #getSubsystemXml()} will be called
     * @param configIdResolvedModel  id to pass to {@link #getSubsystemXml(String)} to get the configuration;
     *                               it is the expected result of resolve() on configId if {@code null}
     ]                               this step is skipped
     *
     * @param compareXml if {@code true} a comparison of xml output to original input is performed. This can be
     *                   set to {@code false} if the original input is from an earlier xsd and the current
     *                   schema has a different output
     *
     * @throws Exception
     */
    protected KernelServices standardSubsystemTest(final String configId, final String configIdResolvedModel, boolean compareXml) throws Exception {
        final AdditionalInitialization additionalInit = createAdditionalInitialization();


        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = configId == null ? getSubsystemXml() : getSubsystemXml(configId);
        final KernelServices servicesA = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemXml).build();
        Assert.assertTrue("Subsystem boot failed!",servicesA.isSuccessfulBoot());
        //Get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        validateModel(modelA);

        // Test marshaling
        final String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();


        // validate the the normalized xmls
        String normalizedSubsystem = normalizeXML(subsystemXml);

        if (compareXml) {
            compareXml(configId, normalizedSubsystem, normalizeXML(marshalled));
        }

        //Install the persisted xml from the first controller into a second controller
        final KernelServices servicesB = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(marshalled).build();
        final ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        compare(modelA, modelB);

        // Test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesB.executeOperation(operation);
        Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem",
                !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
        servicesB.shutdown();

        final KernelServices servicesC = super.createKernelServicesBuilder(additionalInit).setBootOperations(operations).build();
        final ModelNode modelC = servicesC.readWholeModel();

        compare(modelA, modelC);

        assertRemoveSubsystemResources(servicesC, getIgnoredChildResourcesForRemovalTest());

        if (configIdResolvedModel != null) {
            final String subsystemResolvedXml = getSubsystemXml(configIdResolvedModel);
            final KernelServices servicesD = super.createKernelServicesBuilder(additionalInit).setSubsystemXml(subsystemResolvedXml).build();
            Assert.assertTrue("Subsystem w/ reolved xml boot failed!", servicesD.isSuccessfulBoot());
            final ModelNode modelD = servicesD.readWholeModel();
            validateModel(modelD);
            resolveandCompareModel(modelA, modelD);
        }
        return servicesA;

    }

    protected void validateModel(ModelNode model) {
        Assert.assertNotNull(model);
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
        return AdditionalInitialization.MANAGEMENT;
    }

    /**
     * Returns a set of child resources addresses that should not be removed directly. Rather they should be managed
     * by their parent resource
     *
     * @return the set of child resource addresses
     * @see AbstractSubsystemTest#assertRemoveSubsystemResources(KernelServices, Set)
     */
    protected Set<PathAddress> getIgnoredChildResourcesForRemovalTest() {
        return Collections.emptySet();
    }
}
