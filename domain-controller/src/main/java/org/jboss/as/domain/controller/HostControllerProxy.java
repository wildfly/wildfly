/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller;

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class HostControllerProxy implements ProxyController {

    private final String hostName;
    private final HostControllerClient host;

    public HostControllerProxy(final HostControllerClient host) {
        this.hostName = host.getId();
        this.host = host;
    }

    String getHostName() {
        return hostName;
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return PathAddress.EMPTY_ADDRESS;
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(ModelNode operation, ResultHandler handler) {
        return host.execute(operation, handler);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(ModelNode operation) throws CancellationException {
        return host.execute(operation);
    }

}
