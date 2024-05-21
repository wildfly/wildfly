/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#deltaWorkSuccessful(java.util.Map).
 * @author Paul Ferraro
 */
public class DeltaWorkSuccessfulCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -3397082802806176447L;

    private final Address address;

    public DeltaWorkSuccessfulCommand(Address address) {
        this.address = address;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) {
        transport.localDeltaWorkSuccessful(this.address);
        return null;
    }
}
