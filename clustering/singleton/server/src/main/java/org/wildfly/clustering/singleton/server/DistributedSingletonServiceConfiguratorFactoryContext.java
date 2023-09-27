/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public interface DistributedSingletonServiceConfiguratorFactoryContext {
    ServiceName getServiceProviderRegistryServiceName();
    ServiceName getCommandDispatcherFactoryServiceName();
}
