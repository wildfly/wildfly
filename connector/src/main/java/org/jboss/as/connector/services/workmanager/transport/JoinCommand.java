/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#viewAccepted(org.jgroups.View).
 * @author Paul Ferraro
 */
public class JoinCommand implements TransportCommand<Void> {

    private static final long serialVersionUID = 2120774292518363374L;

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.join();
        return null;
    }
}
