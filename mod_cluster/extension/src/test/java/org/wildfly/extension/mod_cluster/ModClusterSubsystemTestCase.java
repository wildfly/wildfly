/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class ModClusterSubsystemTestCase extends AbstractSubsystemSchemaTest<ModClusterSubsystemSchema> {

    @Parameters
    public static Iterable<ModClusterSubsystemSchema> parameters() {
        return EnumSet.allOf(ModClusterSubsystemSchema.class);
    }

    public ModClusterSubsystemTestCase(ModClusterSubsystemSchema schema) {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension(), schema, ModClusterSubsystemSchema.CURRENT);
    }

    @Override
    protected String getSubsystemXmlPathPattern() {
        // N.B. Exclude the subsystem name from pattern
        return "subsystem_%2$d_%3$d.xml";
    }

    @Override
    protected String getSubsystemXsdPathPattern() {
        // N.B. the schema name does not correspond to the subsystem name, so exclude this parameter from pattern
        return "schema/jboss-as-mod-cluster_%2$d_%3$d.xsd";
    }
}
