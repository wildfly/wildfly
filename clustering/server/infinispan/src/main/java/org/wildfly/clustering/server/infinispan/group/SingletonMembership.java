/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.util.Collections;
import java.util.List;

import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

/**
 * A membership that only ever contains a single member.
 * @author Paul Ferraro
 */
public class SingletonMembership implements Membership {

    private final Node member;

    public SingletonMembership(Node member) {
        this.member = member;
    }

    @Override
    public boolean isCoordinator() {
        return true;
    }

    @Override
    public Node getCoordinator() {
        return this.member;
    }

    @Override
    public List<Node> getMembers() {
        return Collections.singletonList(this.member);
    }

    @Override
    public int hashCode() {
        return this.member.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SingletonMembership)) return false;
        SingletonMembership membership = (SingletonMembership) object;
        return this.member.equals(membership.member);
    }

    @Override
    public String toString() {
        return this.member.toString();
    }
}
