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

import java.util.EnumSet;

import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class ModClusterSubsystemTestCase extends ClusteringSubsystemTest<ModClusterSchema> {

    @Parameters
    public static Iterable<ModClusterSchema> parameters() {
        return EnumSet.allOf(ModClusterSchema.class);
    }

    public ModClusterSubsystemTestCase(ModClusterSchema schema) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension(), schema, ModClusterSchema.CURRENT, "subsystem_%d_%d.xml", "schema/jboss-as-mod-cluster_%d_%d.xsd");
    }

    @Override
    protected String[] getSubsystemTemplatePaths() {
        // Need to override since template file name != subsystem name
        return new String[] { "/subsystem-templates/mod_cluster.xml" };
    }
}
