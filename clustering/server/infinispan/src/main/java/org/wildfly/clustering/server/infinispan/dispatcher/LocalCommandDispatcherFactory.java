/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;

/**
 * Non-clustered {@link CommandDispatcherFactory} implementation
 * @author Paul Ferraro
 */
public class LocalCommandDispatcherFactory implements CommandDispatcherFactory {

    private final Group group;

    public LocalCommandDispatcherFactory(Group group) {
        this.group = group;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        return new LocalCommandDispatcher<>(this.group.getLocalMember(), context);
    }
}
