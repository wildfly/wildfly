/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * Tests parsing / booting / marshalling of Infinispan configurations.
 *
 * The current XML configuration is tested, along with supported legacy configurations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
@RunWith(value = Parameterized.class)
public class SubsystemParsingTestCase extends ClusteringSubsystemTest {

    private final InfinispanSchema schema;
    private final int operations;

    public SubsystemParsingTestCase(InfinispanSchema schema, int operations) {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension(), String.format("subsystem-infinispan-%d_%d.xml", schema.major(), schema.minor()));
        this.schema = schema;
        this.operations = operations;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
            { InfinispanSchema.VERSION_1_0, 35 },
            { InfinispanSchema.VERSION_1_1, 35 },
            { InfinispanSchema.VERSION_1_2, 39 },
            { InfinispanSchema.VERSION_1_3, 39 },
            { InfinispanSchema.VERSION_1_4, 39 },
            { InfinispanSchema.VERSION_1_5, 39 },
            { InfinispanSchema.VERSION_2_0, 44 },
            { InfinispanSchema.VERSION_3_0, 44 },
            { InfinispanSchema.VERSION_4_0, 53 },
        };
        return Arrays.asList(data);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new InfinispanSubsystemInitialization()
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "hotrod-server-1", "hotrod-server-2")
                .require(CommonUnaryRequirement.DATA_SOURCE, "ExampleDS")
                .require(JGroupsRequirement.CHANNEL, "maximal-channel")
                ;
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled);
    }

    @Override
    protected void compare(ModelNode model1, ModelNode model2) {
        purgeJGroupsModel(model1);
        purgeJGroupsModel(model2);
        super.compare(model1, model2);
    }

    private static void purgeJGroupsModel(ModelNode model) {
        model.get(JGroupsSubsystemResourceDefinition.PATH.getKey()).remove(JGroupsSubsystemResourceDefinition.PATH.getValue());
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        // Parse the subsystem xml into operations
        List<ModelNode> operations = this.parse(this.createAdditionalInitialization(), getSubsystemXml());

        // Check that we have the expected number of operations
        // one for each resource instance
        Assert.assertEquals(operations.toString(), this.operations, operations.size());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        // Parse the subsystem xml and install into the controller
        KernelServices services = createKernelServicesBuilder().setSubsystemXml(getSubsystemXml()).build();

        // Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();

        Assert.assertTrue(model.get(InfinispanSubsystemResourceDefinition.PATH.getKey()).hasDefined(InfinispanSubsystemResourceDefinition.PATH.getValue()));

        checkLegacyParserStatisticsTrue(model.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair()));
    }

    private void checkLegacyParserStatisticsTrue(ModelNode subsystem) {
        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            for (Property containerProp : subsystem.get(CacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                Assert.assertTrue("cache-container=" + containerProp.getName(),
                        containerProp.getValue().get(CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED.getName()).asBoolean());

                for (String key : containerProp.getValue().keys()) {
                    if (key.endsWith("-cache") && !key.equals("default-cache")) {
                        ModelNode caches = containerProp.getValue().get(key);
                        if (caches.isDefined()) {
                            for (Property cacheProp : caches.asPropertyList()) {
                                Assert.assertTrue("cache-container=" + containerProp.getName() + "," + key + "=" + cacheProp.getName(),
                                        containerProp.getValue().get(CacheResourceDefinition.Attribute.STATISTICS_ENABLED.getName()).asBoolean());
                            }
                        }
                    }
                }
            }
        }
    }

    private KernelServicesBuilder createKernelServicesBuilder() throws Exception {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller
     * started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        // Parse the subsystem xml and install into the first controller
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        // Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();

        String marshalled = servicesA.getPersistedSubsystemXml();

        // Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = this.createKernelServicesBuilder().setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        this.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller
     * started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        // Parse the subsystem xml and install into the first controller
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();
        // Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();

        ModelNode operation = Operations.createDescribeOperation(PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH));
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(operation)).asList();

        // Install the describe options from the first controller into a second controller
        KernelServices servicesB = this.createKernelServicesBuilder().setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        this.compare(modelA, modelB);
    }
}
