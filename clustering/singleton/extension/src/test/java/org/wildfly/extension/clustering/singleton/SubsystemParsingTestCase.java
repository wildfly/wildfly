/*
* JBoss, Home of Professional Open Source.
* Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.extension.clustering.singleton;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public class SubsystemParsingTestCase extends ClusteringSubsystemTest {

    private final int expectedOperationCount;

    public SubsystemParsingTestCase(SingletonDeployerSchema schema, int expectedOperationCount) {
        super(SingletonDeployerExtension.SUBSYSTEM_NAME, new SingletonDeployerExtension(), schema.format("subsystem-singleton-deployment-%d_%d.xml"));
        this.expectedOperationCount = expectedOperationCount;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                { SingletonDeployerSchema.VERSION_1_0, 5 },
        };
        return Arrays.asList(data);
    }

    private KernelServices buildKernelServices() throws Exception {
        return this.buildKernelServices(this.getSubsystemXml());
    }

    private KernelServices buildKernelServices(String xml) throws Exception {
        return this.createKernelServicesBuilder(xml).build();
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    private KernelServicesBuilder createKernelServicesBuilder(String xml) throws XMLStreamException {
        return this.createKernelServicesBuilder().setSubsystemXml(xml);
    }

    @Override
    protected ValidationConfiguration getModelValidationConfiguration() {
        // use this configuration to report any exceptional cases for DescriptionProviders
        return new ValidationConfiguration();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName(RequiredCapability.OUTBOUND_SOCKET_BINDING.getName(), "binding0"),
                RuntimeCapability.buildDynamicCapabilityName(RequiredCapability.OUTBOUND_SOCKET_BINDING.getName(), "binding1"));
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        List<ModelNode> operations = this.parse(this.getSubsystemXml());

        Assert.assertEquals(this.expectedOperationCount, operations.size());
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller
     * started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        KernelServices services = this.buildKernelServices();

        ModelNode modelA = services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();
        ModelNode modelB = this.buildKernelServices(marshalled).readWholeModel();

        this.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller
     * started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        KernelServices services = this.buildKernelServices();

        ModelNode modelA = services.readWholeModel();
        ModelNode operation = Operations.createDescribeOperation(PathAddress.pathAddress(SingletonDeployerResourceDefinition.PATH));
        List<ModelNode> operations = checkResultAndGetContents(services.executeOperation(operation)).asList();

        ModelNode modelB = this.createKernelServicesBuilder().setBootOperations(operations).build().readWholeModel();

        this.compare(modelA, modelB);
    }
}
