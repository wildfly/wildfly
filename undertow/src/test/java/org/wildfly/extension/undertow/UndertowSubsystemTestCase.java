/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests subsystem against configurations for all supported subsystem schema versions.
 * @author Paul Ferraro
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
}
