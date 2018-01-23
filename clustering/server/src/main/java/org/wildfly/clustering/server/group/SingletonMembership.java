/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.server.group;

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
