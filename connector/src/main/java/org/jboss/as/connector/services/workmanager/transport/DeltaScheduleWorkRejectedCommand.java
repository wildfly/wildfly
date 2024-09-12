/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaScheduleWorkRejected(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaScheduleWorkRejectedCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = 5726410485723041645L;

    private final Address address;

    public DeltaScheduleWorkRejectedCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaScheduleWorkRejected(this.address);
        return null;
    }
}
