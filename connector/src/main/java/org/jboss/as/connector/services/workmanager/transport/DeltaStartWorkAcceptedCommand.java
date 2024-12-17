/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaStartWorkAccepted(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaStartWorkAcceptedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -2940831151921131815L;

    private final Address address;

    public DeltaStartWorkAcceptedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaStartWorkAccepted(this.address);
        return null;
    }
}
