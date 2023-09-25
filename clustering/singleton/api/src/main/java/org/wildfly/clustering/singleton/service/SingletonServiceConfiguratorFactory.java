/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceName;

/**
 * Extension of {@link SingletonPolicy} for customizing singleton service behavior.
 * @author Paul Ferraro
 */
public interface SingletonServiceConfiguratorFactory extends SingletonPolicy {

    @Override
    SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name);
}
