/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class ModClusterSubsystemParsingTestCase extends ClusteringSubsystemTest {

    private final ModClusterSchema schema;
    private final int expectedOperationCount;

    public ModClusterSubsystemParsingTestCase(ModClusterSchema schema, int expectedOperationCount) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension(), String.format("subsystem_%d_%d.xml", schema.major(), schema.minor()));
        this.schema = schema;
        this.expectedOperationCount = expectedOperationCount;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                { ModClusterSchema.MODCLUSTER_1_0, 13 },
                { ModClusterSchema.MODCLUSTER_1_1, 13 },
                { ModClusterSchema.MODCLUSTER_1_2, 15 },
                { ModClusterSchema.MODCLUSTER_2_0, 15 },
                { ModClusterSchema.MODCLUSTER_3_0, 14 },
        };
        return Arrays.asList(data);
    }

    /**
     * Tests that the xml is parsed into the correct operations.
     */
    @Test
    public void testParseSubsystem() throws Exception {
        List<ModelNode> operations = this.parse(this.getSubsystemXml());

        Assert.assertEquals(this.expectedOperationCount, operations.size());
    }

    @Override
    protected String getSubsystemXsdPath() {
        return String.format("schema/jboss-as-mod-cluster_%d_%d.xsd", schema.major(), schema.minor());
    }

    @Test
    public void testSubsystemWithSimpleLoadProvider() throws Exception {
        if (schema != ModClusterSchema.CURRENT) return;

        super.standardSubsystemTest("subsystem_2_0_simple-load-provider.xml");
    }
}
