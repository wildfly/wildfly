/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;

/**
 * @author Paul Ferraro
 */
public interface DistributedSingletonServiceContext extends SingletonServiceContext {
    Supplier<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistry();
    Supplier<CommandDispatcherFactory> getCommandDispatcherFactory();
    SingletonElectionPolicy getElectionPolicy();
    int getQuorum();
}
