/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.compat;

import org.jboss.msc.service.ServiceName;

/**
 * Compatibility {@link org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory} extension returning a compatibility {@link SingletonServiceConfigurator}.
 * @author Paul Ferraro
 */
@Deprecated
public interface SingletonServiceConfiguratorFactory extends org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory {

    @Override
    SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name);
}
