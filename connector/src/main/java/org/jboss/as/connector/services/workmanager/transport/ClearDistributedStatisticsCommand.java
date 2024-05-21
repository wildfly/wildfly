/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#clearDistributedStatistics(java.util.Map).
 * @author Paul Ferraro
 */
public class ClearDistributedStatisticsCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -1590205163985739077L;

    private final Address address;

    public ClearDistributedStatisticsCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localClearDistributedStatistics(this.address);
        return null;
    }
}
