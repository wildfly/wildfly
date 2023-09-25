/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;

/**
 * Equivalent to {@link org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#viewAccepted(org.jgroups.View)}.
 * @author Paul Ferraro
 */
public class LeaveCommand implements Command<Void, CommandDispatcherTransport> {
    private static final long serialVersionUID = -3857530778548976078L;

    private final Node member;

    public LeaveCommand(Node member) {
        this.member = member;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.leave(this.member);
        return null;
    }
}
