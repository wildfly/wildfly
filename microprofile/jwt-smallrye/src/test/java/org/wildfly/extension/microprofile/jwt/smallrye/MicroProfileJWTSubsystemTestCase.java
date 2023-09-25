/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import java.util.EnumSet;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Parameterized.class)
public class MicroProfileJWTSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileJWTSubsystemSchema> {
    @Parameters
    public static Iterable<MicroProfileJWTSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileJWTSubsystemSchema.class);
    }

    public MicroProfileJWTSubsystemTestCase(MicroProfileJWTSubsystemSchema schema) {
        super(MicroProfileJWTExtension.SUBSYSTEM_NAME, new MicroProfileJWTExtension(), schema, MicroProfileJWTSubsystemSchema.CURRENT);
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
