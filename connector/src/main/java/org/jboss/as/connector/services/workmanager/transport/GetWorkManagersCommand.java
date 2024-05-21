/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import java.util.Set;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#getAddresses(org.jgroups.Address).
 * @author Paul Ferraro
 */
public class GetWorkManagersCommand implements TransportCommand<Set<Address>> {
    private static final long serialVersionUID = 8595995018539997003L;

    @Override
    public Set<Address> execute(CommandDispatcherTransport transport) {
        return transport.getAddresses(transport.getOwnAddress());
    }
}
