/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class DefaultSingletonServiceBuilderContext implements SingletonServiceBuilderContext, SingletonElectionListener {
    private final SingletonServiceTargetContext context;
    private final AtomicReference<ServiceName> name;
    private final AtomicReference<GroupMember> primaryMember = new AtomicReference<>();
    private Service service = Service.NULL;
    private SingletonElectionPolicy electionPolicy = SingletonElectionPolicy.oldest();
    private SingletonElectionListener electionListener = this;
    private int quorum = 1;

    public DefaultSingletonServiceBuilderContext(SingletonServiceTargetContext context) {
        this(null, context);
    }

    public DefaultSingletonServiceBuilderContext(ServiceName name, SingletonServiceTargetContext context) {
        this.name = new AtomicReference<>(name);
        this.context = context;
    }

    @Override
    public ServiceDependency<ServiceProviderRegistrar<ServiceName, GroupMember>> getServiceProviderRegistrarDependency() {
        return this.context.getServiceProviderRegistrarDependency();
    }

    @Override
    public ServiceDependency<CommandDispatcherFactory<GroupMember>> getCommandDispatcherFactoryDependency() {
        return this.context.getCommandDispatcherFactoryDependency();
    }

    @Override
    public ServiceName getServiceName() {
        return this.name.get();
    }

    @Override
    public Service getService() {
        return this.service;
    }

    @Override
    public SingletonElectionPolicy getElectionPolicy() {
        return this.electionPolicy;
    }

    @Override
    public int getQuorum() {
        return this.quorum;
    }

    @Override
    public SingletonElectionListener getElectionListener() {
        return this.electionListener;
    }

    @Override
    public void setServiceName(ServiceName name) {
        this.name.compareAndSet(null, name);
    }

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public void setElectionListener(SingletonElectionListener electionListener) {
        this.electionListener = electionListener;
    }

    @Override
    public void setElectionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
    }

    @Override
    public void setQuorum(int quorum) {
        if (quorum < 1) {
            throw SingletonLogger.ROOT_LOGGER.invalidQuorum(quorum);
        }
        this.quorum = quorum;
    }

    @Override
    public void elected(List<GroupMember> candidateMembers, GroupMember electedMember) {
        GroupMember localMember = this.context.getServiceProviderRegistrarDependency().get().getGroup().getLocalMember();
        GroupMember previousElectedMember = this.primaryMember.getAndSet(electedMember);

        ServiceName name = this.getServiceName();
        if (electedMember != null) {
            SingletonLogger.ROOT_LOGGER.elected(electedMember.getName(), name.getCanonicalName());
        } else {
            SingletonLogger.ROOT_LOGGER.noPrimaryElected(name.getCanonicalName());
        }
        if (localMember.equals(electedMember)) {
            SingletonLogger.ROOT_LOGGER.startSingleton(name.getCanonicalName());
        } else if (localMember.equals(previousElectedMember)) {
            SingletonLogger.ROOT_LOGGER.stopSingleton(name.getCanonicalName());
        }
    }
}