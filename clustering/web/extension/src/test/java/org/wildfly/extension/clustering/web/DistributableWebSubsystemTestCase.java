/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanDefaultCacheRequirement;

/**
 * Unit test for distributable-web subsystem.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class DistributableWebSubsystemTestCase extends AbstractSubsystemSchemaTest<DistributableWebSubsystemSchema> {

    @Parameters
    public static Iterable<DistributableWebSubsystemSchema> parameters() {
        return EnumSet.allOf(DistributableWebSubsystemSchema.class);
    }

    public DistributableWebSubsystemTestCase(DistributableWebSubsystemSchema schema) {
        super(DistributableWebExtension.SUBSYSTEM_NAME, new DistributableWebExtension(), schema, DistributableWebSubsystemSchema.CURRENT);
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(InfinispanDefaultCacheRequirement.CONFIGURATION, "foo")
                .require(InfinispanCacheRequirement.CONFIGURATION, "foo", "bar")
                .require(InfinispanClientRequirement.REMOTE_CONTAINER, "foo")
                ;
    }
}