/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaStartWorkRejected(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaStartWorkRejectedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -1980521523518562227L;

    private final Address address;

    public DeltaStartWorkRejectedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaStartWorkRejected(this.address);
        return null;
    }
}
