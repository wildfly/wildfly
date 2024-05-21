/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Definition of a backups resource.
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=backups
 *
 * @author Paul Ferraro
 */
public class BackupsResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("backups");

    static final BinaryServiceDescriptor<SitesConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, SitesConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupsResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new BackupResourceDefinition(handler, this.executors).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        for (Resource.ResourceEntry entry : resource.getChildren(BackupResourceDefinition.WILDCARD_PATH.getKey())) {
            String siteName = entry.getName();
            ModelNode backup = entry.getModel();
            builder.addBackup().site(siteName)
                    .backupFailurePolicy(BackupFailurePolicy.valueOf(BackupResourceDefinition.Attribute.FAILURE_POLICY.resolveModelAttribute(context, backup).asString()))
                    .replicationTimeout(BackupResourceDefinition.Attribute.TIMEOUT.resolveModelAttribute(context, backup).asLong())
                    .strategy(BackupStrategy.valueOf(BackupResourceDefinition.Attribute.STRATEGY.resolveModelAttribute(context, backup).asString()))
                    .takeOffline()
                        .afterFailures(BackupResourceDefinition.TakeOfflineAttribute.AFTER_FAILURES.resolveModelAttribute(context, backup).asInt())
                        .minTimeToWait(BackupResourceDefinition.TakeOfflineAttribute.MIN_WAIT.resolveModelAttribute(context, backup).asLong())
            ;
        }
        return CapabilityServiceInstaller.builder(CAPABILITY, builder.create()).build();
    }
}
