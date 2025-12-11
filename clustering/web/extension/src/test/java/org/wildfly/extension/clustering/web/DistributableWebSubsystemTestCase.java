/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.Feature;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;

/**
 * Unit test for distributable-web subsystem.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class DistributableWebSubsystemTestCase extends AbstractSubsystemSchemaTest<DistributableWebSubsystemSchema> {

    private final DistributableWebSubsystemSchema schema;

    @Parameters
    public static Iterable<DistributableWebSubsystemSchema> parameters() {
        return EnumSet.allOf(DistributableWebSubsystemSchema.class);
    }

    public DistributableWebSubsystemTestCase(DistributableWebSubsystemSchema schema) {
        // TODO WFCORE-7416 Eventually simplify by using this constructor AbstractSubsystemSchemaTest(String, Extension, S, Set<S>)
        super(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new DistributableWebExtension(), schema, Feature.map(DistributableWebSubsystemSchema.CURRENT).get(schema.getStability()));

        this.schema = schema;
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(this.schema)
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION, "foo")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "foo", "bar")
                .require(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, "foo")
                ;
    }
}