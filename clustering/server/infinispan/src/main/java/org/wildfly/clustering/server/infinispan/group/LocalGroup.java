/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

/**
 * Non-clustered group implementation.
 * Registered {@link GroupListener} are never invoked, as membership of a local group is fixed.
 * @author Paul Ferraro
 */
public class LocalGroup implements AutoCloseableGroup<Object>, Registration {

    private final Membership membership;
    private final String name;

    public LocalGroup(String nodeName, String groupName) {
        this.membership = new SingletonMembership(new LocalNode(nodeName));
        this.name = groupName;
    }

    @Override
    public void close() {
        // We never registered anything
    }

    @Override
    public Registration register(GroupListener listener) {
        // Nothing to register
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Node getLocalMember() {
        return this.membership.getCoordinator();
    }

    @Override
    public Membership getMembership() {
        return this.membership;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Node createNode(Object ignored) {
        return this.getLocalMember();
    }
}
