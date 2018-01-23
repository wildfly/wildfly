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
import java.util.Properties;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
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
public class InfinispanSubsystemParsingTestCase extends ClusteringSubsystemTest {

    private final InfinispanSchema schema;
    private final int operations;

    public InfinispanSubsystemParsingTestCase(InfinispanSchema schema, int operations) {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension(), String.format("subsystem-infinispan-%d_%d.xml", schema.major(), schema.minor()));
        this.schema = schema;
        this.operations = operations;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
            { InfinispanSchema.VERSION_1_0, 33 },
            { InfinispanSchema.VERSION_1_1, 33 },
            { InfinispanSchema.VERSION_1_2, 37 },
            { InfinispanSchema.VERSION_1_3, 37 },
            { InfinispanSchema.VERSION_1_4, 37 },
            { InfinispanSchema.VERSION_1_5, 37 },
            { InfinispanSchema.VERSION_2_0, 41 },
            { InfinispanSchema.VERSION_3_0, 41 },
            { InfinispanSchema.VERSION_4_0, 50 },
            { InfinispanSchema.VERSION_5_0, 50 },
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
    protected void compare(ModelNode model1, ModelNode model2) {
        purgeJGroupsModel(model1);
        purgeJGroupsModel(model2);
        super.compare(model1, model2);
    }

    private static void purgeJGroupsModel(ModelNode model) {
        model.get(JGroupsSubsystemResourceDefinition.PATH.getKey()).remove(JGroupsSubsystemResourceDefinition.PATH.getValue());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format("schema/jboss-as-infinispan_%d_%d.xsd", this.schema.major(), this.schema.minor());
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("java.io.tmpdir", "/tmp");
        return properties;
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
     * Does the normal test with some extra model validation.
     */
    @Override
    @Test
    public void testSubsystem() throws Exception {
        // Perform the standard test
        KernelServices services = standardSubsystemTest(null, true);

        // Do some extra model validation
        checkLegacyParserStatisticsTrue(services);
    }

    private void checkLegacyParserStatisticsTrue(KernelServices services) {
        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            ModelNode model = services.readWholeModel();

            Assert.assertTrue(model.get(InfinispanSubsystemResourceDefinition.PATH.getKey()).hasDefined(InfinispanSubsystemResourceDefinition.PATH.getValue()));
            ModelNode subsystem = model.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair());

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
}
