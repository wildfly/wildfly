/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Factory for creating a singleton service builder.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link SingletonServiceConfiguratorFactory}
 */
@Deprecated(forRemoval = true)
public interface SingletonServiceBuilderFactory extends SingletonPolicy, SingletonServiceConfiguratorFactory {
    UnaryServiceDescriptor<SingletonServiceBuilderFactory> DEFAULT_SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.cache.default-singleton-service-builder-factory", SingletonServiceBuilderFactory.class);
    BinaryServiceDescriptor<SingletonServiceBuilderFactory> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.cache.singleton-service-builder-factory", DEFAULT_SERVICE_DESCRIPTOR);

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service);

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService);
}
