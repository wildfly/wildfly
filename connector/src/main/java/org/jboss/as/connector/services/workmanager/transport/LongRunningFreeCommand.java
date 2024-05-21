/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#getLongRunningFree(java.util.Map).
 * @author Paul Ferraro
 */
public class LongRunningFreeCommand implements TransportCommand<Long> {
    private static final long serialVersionUID = -3552549556601333089L;

    private final Address address;

    public LongRunningFreeCommand(Address address) {
        this.address = address;
    }

    @Override
    public Long execute(CommandDispatcherTransport transport) {
        return transport.localGetLongRunningFree(this.address);
    }
}
