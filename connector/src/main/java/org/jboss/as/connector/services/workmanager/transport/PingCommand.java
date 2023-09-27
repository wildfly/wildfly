/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Equivalent to {@link org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#localPing()}.
 * @author Paul Ferraro
 */
public class PingCommand implements Command<Long, CommandDispatcherTransport> {
    private static final long serialVersionUID = 7747022347047976535L;

    @Override
    public Long execute(CommandDispatcherTransport transport) {
        return transport.localPing();
    }
}
