/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import jakarta.resource.spi.work.DistributableWork;
import jakarta.resource.spi.work.WorkException;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#startWork(Address, DistributableWork).
 * @author Paul Ferraro
 */
public class StartWorkCommand implements TransportCommand<Long> {
    private static final long serialVersionUID = -661447249010320508L;

    private final Address address;
    private final DistributableWork work;

    public StartWorkCommand(Address address, DistributableWork work) {
        this.address = address;
        this.work = work;
    }

    @Override
    public Long execute(CommandDispatcherTransport transport) throws WorkException {
        return transport.localStartWork(this.address, this.work);
    }
}
