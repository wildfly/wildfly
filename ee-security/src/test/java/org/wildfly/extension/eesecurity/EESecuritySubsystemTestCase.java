/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.eesecurity;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Parameterized.class)
public class EESecuritySubsystemTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<EESecuritySubsystemSchema> parameters() {
        return EnumSet.allOf(EESecuritySubsystemSchema.class);
    }

    private final EESecuritySubsystemSchema schema;

    public EESecuritySubsystemTestCase(EESecuritySubsystemSchema schema) {
        super(EESecurityExtension.SUBSYSTEM_NAME, new EESecurityExtension());
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return String.format(Locale.ROOT, "<subsystem xmlns=\"%s\"/>", this.schema.getNamespace());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format(Locale.ROOT, "schema/wildfly-ee-security_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }

    //no point in testing 1.0.0 (current) --> 1.0.0 (all previous) for transformers
}
