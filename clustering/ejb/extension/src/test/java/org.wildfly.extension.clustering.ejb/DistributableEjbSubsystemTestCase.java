/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;

import java.util.EnumSet;

/**
 * Unit test for distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@RunWith(value = Parameterized.class)
public class DistributableEjbSubsystemTestCase extends ClusteringSubsystemTest<DistributableEjbSchema> {

    @Parameters
    public static Iterable<DistributableEjbSchema> parameters() {
        return EnumSet.allOf(DistributableEjbSchema.class);
    }

    public DistributableEjbSubsystemTestCase(DistributableEjbSchema schema) {
        super(DistributableEjbExtension.SUBSYSTEM_NAME, new DistributableEjbExtension(), schema, "wildfly-distributable-ejb-%d_%d.xml", "schema/wildfly-distributable-ejb_%d_%d.xsd");
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(InfinispanDefaultCacheRequirement.CONFIGURATION, "foo")
                .require(InfinispanCacheRequirement.CONFIGURATION, "foo", "bar")
                ;
    }

    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        // Skip
    }
}