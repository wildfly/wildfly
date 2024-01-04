/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import java.util.EnumSet;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2019 Red Hat inc.
 */
@RunWith(Parameterized.class)
public class HealthSubsystemTestCase extends AbstractSubsystemSchemaTest<HealthSubsystemSchema> {
    @Parameters
    public static Iterable<HealthSubsystemSchema> parameters() {
        return EnumSet.allOf(HealthSubsystemSchema.class);
    }

    public HealthSubsystemTestCase(HealthSubsystemSchema schema) {
        super(HealthExtension.SUBSYSTEM_NAME, new HealthExtension(), schema, HealthSubsystemSchema.CURRENT);
    }

    @Override
    protected String getSubsystemXmlPathPattern() {
        // Exclude subsystem name from pattern
        return "subsystem_%2$d_%3$d.xml";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }
}