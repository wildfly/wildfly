/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Jakarta Bean Validation subsystem tests.
 *
 * @author Eduardo Martins
 */
@RunWith(Parameterized.class)
public class BeanValidationSubsystemTestCase extends AbstractSubsystemSchemaTest<BeanValidationSubsystemSchema> {
    @Parameters
    public static Iterable<BeanValidationSubsystemSchema> parameters() {
        return EnumSet.allOf(BeanValidationSubsystemSchema.class);
    }

    public BeanValidationSubsystemTestCase(BeanValidationSubsystemSchema schema) {
        super(BeanValidationExtension.SUBSYSTEM_NAME, new BeanValidationExtension(), schema, BeanValidationSubsystemSchema.CURRENT);
    }
}
