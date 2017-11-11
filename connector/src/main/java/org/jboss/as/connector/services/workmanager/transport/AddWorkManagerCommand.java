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

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;

/**
 * Equivalent to {@link org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#addWorkManager(java.util.Map, org.jgroups.Address)}.
 * @author Paul Ferraro
 */
public class AddWorkManagerCommand implements Command<Void, CommandDispatcherTransport> {
    private static final long serialVersionUID = -6747024371979702527L;

    private final Address address;
    private final Node member;

    public AddWorkManagerCommand(Address address, Node member) {
        this.address = address;
        this.member = member;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localWorkManagerAdd(this.address, this.member);

        transport.localUpdateShortRunningFree(this.address, transport.getShortRunningFree(this.address));
        transport.localUpdateLongRunningFree(this.address, transport.getLongRunningFree(this.address));
        return null;
    }
}
