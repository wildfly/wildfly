/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import jakarta.resource.spi.work.DistributableWork;
import jakarta.resource.spi.work.WorkException;

import org.jboss.jca.core.spi.workmanager.Address;

/**
 * Equivalent to org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport#doWork(Address, DistributableWork).
 * @author Paul Ferraro
 */
public class DoWorkCommand implements TransportCommand<Void> {
    private static final long serialVersionUID = -661447249010320508L;

    private final Address address;
    private final DistributableWork work;

    public DoWorkCommand(Address address, DistributableWork work) {
        this.address = address;
        this.work = work;
    }

    @Override
    public Void execute(CommandDispatcherTransport transport) throws WorkException {
        transport.localDoWork(this.address, this.work);
        return null;
    }
}
