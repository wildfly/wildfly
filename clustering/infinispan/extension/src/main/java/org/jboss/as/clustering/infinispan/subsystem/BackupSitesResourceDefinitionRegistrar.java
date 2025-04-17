/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a backups cache component resource definition.
 *
 * @author Paul Ferraro
 */
public class BackupSitesResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<SitesConfiguration, SitesConfigurationBuilder> {

    static final BinaryServiceDescriptor<SitesConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptorFactory.createServiceDescriptor(ComponentResourceRegistration.BACKUP_SITES, SitesConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupSitesResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return ComponentResourceRegistration.BACKUP_SITES;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new BackupSiteResourceDefinitionRegistrar(this.get(), this.executors).register(registration, context);

        return registration;
    }

    @Override
    public ServiceDependency<SitesConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> sites = resource.getChildrenNames(BackupSiteResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey());
        List<ServiceDependency<BackupConfiguration>> backups = new ArrayList<>(sites.size());
        for (String site : sites) {
            backups.add(ServiceDependency.on(BackupSiteResourceDefinitionRegistrar.SERVICE_DESCRIPTOR, containerName, cacheName, site));
        }
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                for (ServiceDependency<BackupConfiguration> backup : backups) {
                    backup.accept(builder);
                }
            }

            @Override
            public SitesConfigurationBuilder get() {
                SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
                for (ServiceDependency<BackupConfiguration> backup : backups) {
                    builder.addBackup().read(backup.get());
                }
                return builder;
            }
        };
    }
}
