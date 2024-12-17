/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#updateLongRunningFree(java.util.Map, Long).
 * @author Paul Ferraro
 */
public class UpdateLongRunningFreeCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -3887902059566674034L;

    private final Address address;
    private final long free;

    public UpdateLongRunningFreeCommand(Address address, long free) {
        this.address = address;
        this.free = free;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localUpdateLongRunningFree(this.address, this.free);
        return null;
    }
}
