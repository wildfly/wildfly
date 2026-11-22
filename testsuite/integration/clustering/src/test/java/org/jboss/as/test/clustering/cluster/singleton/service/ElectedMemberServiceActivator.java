/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceActivator;
import org.wildfly.clustering.singleton.service.SingletonServiceActivatorContext;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;

/**
 * @author Paul Ferraro
 */
public class ElectedMemberServiceActivator implements SingletonServiceActivator {

    public static final ServiceName DEFAULT_SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "default");
    public static final ServiceName QUORUM_SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "quorum");

    public static final String PREFERRED_NODE = NODE_2;

    @Override
    public void activate(SingletonServiceActivatorContext context) {
        install(context.getServiceTarget(), DEFAULT_SERVICE_NAME, 1);
        install(context.getServiceTarget(), QUORUM_SERVICE_NAME, 2);
    }

    private static void install(SingletonServiceTarget target, ServiceName name, int quorum) {
        Consumer<GroupMember> elected = ElectedMemberRegistry.INSTANCE.add(name);
        SingletonServiceBuilder<?> builder = target.addService();
        builder.provides(name);
        builder.addListener(new LifecycleListener() {
                @Override
                public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                    if (event == LifecycleEvent.REMOVED) {
                        ElectedMemberRegistry.INSTANCE.remove(name);
                    }
                }
            })
            .requireQuorum(quorum)
            .withElectionPolicy(SingletonElectionPolicy.oldest().prefer(List.of(member -> member.getName().equals(PREFERRED_NODE))))
            .withElectionListener(new SingletonElectionListener() {
                @Override
                public void elected(List<GroupMember> candidateMembers, GroupMember electedMember) {
                    elected.accept(electedMember);
                }
            })
            .install();
    }
}
