/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

/**
 * @author Paul Ferraro
 */
public interface SingletonServiceContext {
    ServiceName getServiceName();
    Service getService();
    SingletonElectionListener getElectionListener();
    SingletonElectionPolicy getElectionPolicy();
    int getQuorum();
    ServiceProviderRegistrar<ServiceName, GroupMember> getServiceProviderRegistrar();
    CommandDispatcherFactory<GroupMember> getCommandDispatcherFactory();
}
