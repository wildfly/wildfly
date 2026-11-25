/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#workManagerRemove(java.util.Map).
 * @author Paul Ferraro
 */
public class RemoveWorkManagerCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = 2582985650458275860L;

    private final Address address;

    public RemoveWorkManagerCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localWorkManagerRemove(this.address);
        return null;
    }
}
