/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.pojo;

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
public class PojoSubsystemTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<PojoSubsystemSchema> parameters() {
        return EnumSet.allOf(PojoSubsystemSchema.class);
    }

    private final PojoSubsystemSchema schema;

    public PojoSubsystemTestCase(PojoSubsystemSchema schema) {
        super(PojoExtension.SUBSYSTEM_NAME, new PojoExtension());
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return String.format(Locale.ROOT, "<subsystem xmlns=\"%s\" />", this.schema.getNamespace());
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return String.format(Locale.ROOT, "schema/jboss-as-pojo_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }
}
