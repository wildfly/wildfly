/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.service;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Parameterized.class)
public class SarSubsystemTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<SarSubsystemSchema> parameters() {
        return EnumSet.allOf(SarSubsystemSchema.class);
    }

    private static final AdditionalInitialization ADDITIONAL_INITIALIZATION = AdditionalInitialization.withCapabilities("org.wildfly.management.jmx");

    private final SarSubsystemSchema schema;

    public SarSubsystemTestCase(SarSubsystemSchema schema) {
        super(SarExtension.SUBSYSTEM_NAME, new SarExtension());
        this.schema = schema;
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return ADDITIONAL_INITIALIZATION;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return String.format(Locale.ROOT, "<subsystem xmlns=\"%s\"/>", this.schema.getNamespace());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format(Locale.ROOT, "schema/jboss-as-sar_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }
}