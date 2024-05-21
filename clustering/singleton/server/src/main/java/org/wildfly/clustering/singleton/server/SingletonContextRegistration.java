/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;

/**
 * @author Paul Ferraro
 */
public interface SingletonContextRegistration<C extends SingletonContext> extends SingletonRegistration, SingletonContext {

    CommandDispatcher<GroupMember, C> getCommandDispatcher();

    ServiceProviderRegistration<ServiceName, GroupMember> getServiceProviderRegistration();

    @Override
    default void close() {
        this.getServiceProviderRegistration().close();
        this.getCommandDispatcher().close();
    }
}
