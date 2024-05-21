/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.compat;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;

/**
 * Compatibility {@link org.wildfly.clustering.singleton.SingletonServiceBuilderFactory} extension returning a compatibility {@link SingletonServiceBuilder}.
 * @author Paul Ferraro
 */
@Deprecated
public interface SingletonServiceBuilderFactory extends org.wildfly.clustering.singleton.SingletonServiceBuilderFactory {

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service);

    @Override
    <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService);
}
