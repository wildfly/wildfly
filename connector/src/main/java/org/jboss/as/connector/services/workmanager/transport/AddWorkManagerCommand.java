/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;
import org.wildfly.clustering.server.GroupMember;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#addWorkManager(java.util.Map, org.jgroups.Address).
 * @author Paul Ferraro
 */
public class AddWorkManagerCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -6747024371979702527L;

    private final Address address;
    private final GroupMember member;

    public AddWorkManagerCommand(Address address, GroupMember member) {
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
