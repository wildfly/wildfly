/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedStateCacheResourceDefinitionRegistrar extends CacheResourceDefinitionRegistrar<ClusteredCacheServiceInstallerProvider> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    SharedStateCacheResourceDefinitionRegistrar(SharedStateCacheResourceDescription description, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(description);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new ComponentResourceDefinitionRegistrar<>(PartitionHandlingResourceDescription.INSTANCE).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(StateTransferResourceDescription.INSTANCE).register(registration, context);
        new BackupSitesResourceDefinitionRegistrar(this.executors).register(registration, context);

        return registration;
    }
}
