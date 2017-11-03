/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;

/**
 * Non-clustered {@link Group} implementation
 * @author Paul Ferraro
 */
public class LocalGroup implements Group {

    private final String name;
    private final Membership membership;

    public LocalGroup(String name, Node node) {
        this.name = name;
        this.membership = new SingletonMembership(node);
    }

    @Override
    public Registration register(GroupListener object) {
        // membership of a non-clustered group will never change
        return () -> {};
    }

    @Deprecated
    @Override
    public void removeListener(Listener listener) {
        // membership of a non-clustered group will never change
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
}
