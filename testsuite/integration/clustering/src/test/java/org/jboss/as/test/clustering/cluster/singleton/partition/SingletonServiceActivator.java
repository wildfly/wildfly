/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.partition;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_2;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberRegistry;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceActivatorContext;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;

/**
 * @author Tomas Hofman
 */
public class SingletonServiceActivator implements org.wildfly.clustering.singleton.service.SingletonServiceActivator {

    public static final ServiceName SERVICE_A_NAME = ServiceName.JBOSS.append("test1", "service", "default");
    public static final ServiceName SERVICE_B_NAME = ServiceName.JBOSS.append("test2", "service", "default");
    public static final String SERVICE_A_PREFERRED_NODE = NODE_2;
    public static final String SERVICE_B_PREFERRED_NODE = NODE_1;

    @Override
    public void activate(SingletonServiceActivatorContext context) {
        install(context.getServiceTarget(), SERVICE_A_NAME, SERVICE_A_PREFERRED_NODE);
        install(context.getServiceTarget(), SERVICE_B_NAME, SERVICE_B_PREFERRED_NODE);
    }

    private static void install(SingletonServiceTarget target, ServiceName name, String preferredMember) {
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
            .withElectionPolicy(SingletonElectionPolicy.oldest().prefer(List.of(member -> member.getName().equals(preferredMember))))
            .withElectionListener(new SingletonElectionListener() {
                @Override
                public void elected(List<GroupMember> candidateMembers, GroupMember electedMember) {
                    elected.accept(electedMember);
                }
            })
            .install();
    }
}
