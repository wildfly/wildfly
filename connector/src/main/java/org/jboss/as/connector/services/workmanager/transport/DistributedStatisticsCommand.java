/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.api.workmanager.DistributedWorkManagerStatisticsValues;
import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#getDistributedStatistics(java.util.Map).
 * @author Paul Ferraro
 */
public class DistributedStatisticsCommand implements TransportCommand<DistributedWorkManagerStatisticsValues> {
    private static final long serialVersionUID = -8884303103746998259L;

    private final Address address;

    public DistributedStatisticsCommand(Address address) {
        this.address = address;
    }

    @Override
    public DistributedWorkManagerStatisticsValues execute(CommandDispatcherTransport transport) {
        return transport.localGetDistributedStatistics(this.address);
    }
}
