/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaDoWorkAccepted(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaDoWorkAcceptedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -2256088009794548082L;

    private final Address address;

    public DeltaDoWorkAcceptedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaDoWorkAccepted(this.address);
        return null;
    }
}
