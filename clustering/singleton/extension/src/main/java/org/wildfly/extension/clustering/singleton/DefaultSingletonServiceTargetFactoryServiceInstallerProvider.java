/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DefaultCacheServiceInstallerProvider.class)
public class DefaultSingletonServiceTargetFactoryServiceInstallerProvider implements DefaultCacheServiceInstallerProvider {

    @SuppressWarnings({ "deprecation", "removal" })
    @Override
    public Iterable<ServiceInstaller> apply(BinaryServiceConfiguration configuration) {
        BinaryServiceConfiguration defaultConfiguration = configuration.withChildName(null);
        return List.of(ServiceInstaller.builder(configuration.getServiceDependency(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR))
                .provides(defaultConfiguration.resolveServiceName(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR))
                .provides(defaultConfiguration.resolveServiceName(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR))
                .provides(defaultConfiguration.resolveServiceName(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR))
                .build());
    }
}
