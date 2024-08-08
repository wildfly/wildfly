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
public interface SingletonServiceBuilderContext extends SingletonServiceTargetContext, SingletonServiceContext {

    void setServiceName(ServiceName name);

    void setService(Service service);

    void setElectionListener(SingletonElectionListener electionListener);

    void setElectionPolicy(SingletonElectionPolicy electionPolicy);

    void setQuorum(int quorum);

    @Override
    default ServiceProviderRegistrar<ServiceName, GroupMember> getServiceProviderRegistrar() {
        return this.getServiceProviderRegistrarDependency().get();
    }

    @Override
    default CommandDispatcherFactory<GroupMember> getCommandDispatcherFactory() {
        return this.getCommandDispatcherFactoryDependency().get();
    }
}
