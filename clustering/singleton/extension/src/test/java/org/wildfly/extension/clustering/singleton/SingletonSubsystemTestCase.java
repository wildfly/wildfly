/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;

/**
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public class SingletonSubsystemTestCase extends AbstractSubsystemSchemaTest<SingletonSubsystemSchema> {

    @Parameters
    public static Iterable<SingletonSubsystemSchema> parameters() {
        return EnumSet.allOf(SingletonSubsystemSchema.class);
    }

    public SingletonSubsystemTestCase(SingletonSubsystemSchema schema) {
        super(SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new SingletonExtension(), schema, SingletonSubsystemSchema.CURRENT);
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding0")
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding1")
                .require(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR, "singleton-container")
                .require(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR, "singleton-container", "singleton-cache")
                ;
    }
}
