/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

/**
 * Abstract non-clustered group implementation.
 * Registered {@link GroupListener} are never invoked, as membership of a local group is fixed.
 * @author Paul Ferraro
 */
public abstract class AbstractLocalGroup<A> implements Group<A>, Registration {
    private static final String NAME = "local";

    private final Membership membership;

    public AbstractLocalGroup(String nodeName) {
        this.membership = new SingletonMembership(new LocalNode(nodeName));
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

    @Deprecated
    @Override
    public void removeListener(org.wildfly.clustering.group.Group.Listener listener) {
        // We never registered anything
    }

    @Override
    public String getName() {
        return NAME;
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
    public Node createNode(A address) {
        return this.getLocalMember();
    }
}
