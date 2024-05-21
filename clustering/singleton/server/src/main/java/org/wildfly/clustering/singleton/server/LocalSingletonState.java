/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.Optional;
import java.util.Set;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.SingletonState;

/**
 * Singleton state for a singleton group.
 * @author Paul Ferraro
 */
public class LocalSingletonState implements SingletonState {

    private final GroupMember localMember;

    public LocalSingletonState(GroupMember localMember) {
        this.localMember = localMember;
    }

    @Override
    public boolean isPrimaryProvider() {
        return true;
    }

    @Override
    public Optional<GroupMember> getPrimaryProvider() {
        return Optional.of(this.localMember);
    }

    @Override
    public Set<GroupMember> getProviders() {
        return Set.of(this.localMember);
    }
}
