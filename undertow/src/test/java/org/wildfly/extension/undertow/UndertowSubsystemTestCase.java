/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;

import org.jboss.as.version.Stability;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests subsystem against configurations for all supported subsystem schema versions.
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class UndertowSubsystemTestCase extends AbstractUndertowSubsystemTestCase {

    @Parameters
    public static Iterable<UndertowSubsystemSchema> parameters() {
        return EnumSet.allOf(UndertowSubsystemSchema.class);
    }

    public UndertowSubsystemTestCase(UndertowSubsystemSchema schema) {
        super(schema);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        // n.b. for preview:14 schema subsystem test, we automatically promote stability to a community:14 schema since they are now effectively equivalent;
        // thus for this particular schema we need to ignore comparison of the namespace
        boolean ignoreNamespace = schema.getStability() == Stability.PREVIEW && schema.getVersion().major() == 14;
        super.compareXml(configId, original, marshalled, ignoreNamespace);
    }
}
