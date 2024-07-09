/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#viewAccepted(org.jgroups.View).
 * @author Paul Ferraro
 */
public class JoinCommand implements Command<Void, CommandDispatcherTransport> {

    private static final long serialVersionUID = 2120774292518363374L;

    @Override
    public Void execute(CommandDispatcherTransport transport) throws Exception {
        transport.join();
        return null;
    }
}
