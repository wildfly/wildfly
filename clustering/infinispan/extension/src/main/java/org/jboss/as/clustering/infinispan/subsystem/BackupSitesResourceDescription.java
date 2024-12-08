/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public enum BackupSitesResourceDescription implements CacheComponentResourceDescription<SitesConfiguration, SitesConfigurationBuilder> {
    INSTANCE;

    private final PathElement path = ComponentResourceDescription.pathElement("backups");

    private final BinaryServiceDescriptor<SitesConfiguration> descriptor = CacheComponentResourceDescription.createServiceDescriptor(this.path, SitesConfiguration.class);
    private final RuntimeCapability<Void> capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public BinaryServiceDescriptor<SitesConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.empty();
    }

    @Override
    public ServiceDependency<SitesConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> sites = resource.getChildrenNames(BackupSiteResourceDescription.INSTANCE.getPathElement().getKey());
        List<ServiceDependency<BackupConfiguration>> backups = new ArrayList<>(sites.size());
        for (String site : sites) {
            backups.add(ServiceDependency.on(BackupSiteResourceDescription.INSTANCE.getServiceDescriptor(), containerName, cacheName, site));
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
