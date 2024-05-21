/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaScheduleWorkAccepted(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaScheduleWorkAcceptedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -3878717003141874003L;

    private final Address address;

    public DeltaScheduleWorkAcceptedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaScheduleWorkAccepted(this.address);
        return null;
    }
}
