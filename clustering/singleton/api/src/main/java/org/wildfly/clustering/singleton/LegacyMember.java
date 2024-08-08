/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import java.net.InetSocketAddress;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.GroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated(forRemoval = true)
class LegacyMember<M extends GroupMember> implements Node {
    private final M member;

    LegacyMember(M member) {
        this.member = member;
    }

    @Override
    public String getName() {
        return this.member.getName();
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return null;
    }

    @Override
    public int hashCode() {
        return this.member.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LegacyMember)) return false;
        return this.member.equals(((LegacyMember<M>) object).member);
    }

    @Override
    public String toString() {
        return this.member.toString();
    }
}
