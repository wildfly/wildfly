/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.provider.DefaultLocalServiceProviderRegistrar;
import org.wildfly.clustering.server.local.provider.LocalServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.BlockingLifecycle;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistrarServiceInstallerFactory<T> extends ServiceProviderRegistrarServiceInstallerFactory<T> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<LocalGroup> group = ServiceDependency.on(ClusteringServiceDescriptor.GROUP, ModelDescriptionConstants.LOCAL).map(LocalGroup.class::cast);
        ServiceDependency<SuspendableActivityRegistrar> activityRegistrar = ServiceDependency.on(SuspendableActivityRegistrar.SERVICE_DESCRIPTOR);
        UnaryOperator<LocalServiceProviderRegistrar<T>> decorator = new UnaryOperator<>() {
            @Override
            public LocalServiceProviderRegistrar<T> apply(LocalServiceProviderRegistrar<T> registrar) {
                return new SuspendableLocalServiceProviderRegistrar<>(registrar, activityRegistrar.get());
            }
        };
        return ServiceInstaller.BlockingBuilder.of(group.<DefaultLocalServiceProviderRegistrar<T>>map(DefaultLocalServiceProviderRegistrar::new))
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(List.of(activityRegistrar))
                .withLifecycle(BlockingLifecycle.autoClose())
                .map(decorator)
                .build();
    }
}
