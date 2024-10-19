/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a backups cache component resource definition.
 *
 * @author Paul Ferraro
 */
public class BackupSitesResourceDefinitionRegistrar extends ComponentResourceDefinitionRegistrar<SitesConfiguration, SitesConfigurationBuilder> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupSitesResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(BackupSitesResourceDescription.INSTANCE);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new BackupSiteResourceDefinitionRegistrar(ResourceOperationRuntimeHandler.configureService(this), this.executors).register(registration, context);

        return registration;
    }
}
