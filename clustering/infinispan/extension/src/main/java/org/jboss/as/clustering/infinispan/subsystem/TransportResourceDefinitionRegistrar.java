/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;

import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBiServiceInstallerProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Registers a transport resource definition.
 * @author Paul Ferraro
 */
public class TransportResourceDefinitionRegistrar extends ComponentResourceDefinitionRegistrar<TransportConfiguration, TransportConfigurationBuilder> {

    private final TransportResourceDescription description;

    public TransportResourceDefinitionRegistrar(TransportResourceDescription description) {
        super(description);
        this.description = description;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        String groupName = this.description.resolveGroupName(context, model);

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(super.configure(context, model));

        new ProvidedBiServiceInstallerProvider<>(CacheContainerServiceInstallerProvider.class, CacheContainerServiceInstallerProvider.class.getClassLoader()).apply(name, groupName).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
