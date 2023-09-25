/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.singleton.SingletonCacheRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;

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
        super(SingletonExtension.SUBSYSTEM_NAME, new SingletonExtension(), schema, SingletonSubsystemSchema.CURRENT);
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "binding0", "binding1")
                .require(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container")
                .require(SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container", "singleton-cache")
                ;
    }
}
