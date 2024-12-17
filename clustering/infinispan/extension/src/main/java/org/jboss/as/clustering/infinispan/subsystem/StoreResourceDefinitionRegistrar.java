/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class StoreResourceDefinitionRegistrar<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends ComponentResourceDefinitionRegistrar<PersistenceConfiguration, PersistenceConfigurationBuilder> {

    StoreResourceDefinitionRegistrar(StoreResourceDescription<C, B> description) {
        super(description);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new ComponentResourceDefinitionRegistrar<>(StoreWriteBehindResourceDescription.INSTANCE).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(StoreWriteThroughResourceDescription.INSTANCE).register(registration, context);

        return registration;
    }
}
