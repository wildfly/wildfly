/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBiServiceInstallerProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class NoTransportResourceDefinition extends TransportResourceDefinition {
    static final PathElement PATH = pathElement("none");

    NoTransportResourceDefinition() {
        super(PATH, UnaryOperator.identity());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String containerName = containerAddress.getLastElement().getValue();

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        Supplier<TransportConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public TransportConfiguration get() {
                return new GlobalConfigurationBuilder().transport().transport(null)
                        .clusterName(ModelDescriptionConstants.LOCAL)
                        .nodeName(environment.get().getNodeName())
                        .create();
            }
        };
        installers.add(CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory).requires(environment).build());

        new ProvidedBiServiceInstallerProvider<>(CacheContainerServiceInstallerProvider.class, CacheContainerServiceInstallerProvider.class.getClassLoader()).apply(containerName, ModelDescriptionConstants.LOCAL).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
