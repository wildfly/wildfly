/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Factory for creating a singleton service builder.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link SingletonServiceConfiguratorFactory}
 */
@Deprecated(forRemoval = true)
public interface SingletonServiceBuilderFactory extends SingletonPolicy, SingletonServiceConfiguratorFactory {

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service);

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService);
}
