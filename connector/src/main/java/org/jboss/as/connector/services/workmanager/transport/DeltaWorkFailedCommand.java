/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaWorkFailed(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaWorkFailedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = 8092302980939329432L;

    private final Address address;

    public DeltaWorkFailedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaWorkFailed(this.address);
        return null;
    }
}
