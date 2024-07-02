/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#localPing().
 * @author Paul Ferraro
 */
public class PingCommand implements TransportCommand<Long> {
    private static final long serialVersionUID = 7747022347047976535L;

    @Override
    public Long execute(CommandDispatcherTransport transport) {
        return transport.localPing();
    }
}
