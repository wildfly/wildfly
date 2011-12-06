/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.io.IOException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.OptionMap;

/**
 * An MSC service for Remoting endpoints.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EndpointService implements Service<Endpoint> {

    private final String endpointName;
    private Endpoint endpoint;
    private OptionMap optionMap;

    public EndpointService(String nodeName, EndpointType type) {
        if (nodeName == null) {
            nodeName = "remote";
        }
        endpointName = type == EndpointType.SUBSYSTEM ? nodeName : nodeName + ":" + type;
    }

    /**
     * Set the option map for this endpoint.
     *
     * @param optionMap the option map
     */
    public synchronized void setOptionMap(final OptionMap optionMap) {
        this.optionMap = optionMap;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final Endpoint endpoint;
        try {
            boolean ok = false;
            endpoint = Remoting.createEndpoint(endpointName, optionMap);
            try {
                endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);
                ok = true;
            } finally {
                if (! ok) {
                    endpoint.closeAsync();
                }
            }
        } catch (IOException e) {
            throw new StartException("Failed to start service", e);
        }
        this.endpoint = endpoint;
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        context.asynchronous();
        try {
            endpoint.closeAsync();
        } finally {
            endpoint.addCloseHandler(new CloseHandler<Endpoint>() {
                public void handleClose(final Endpoint closed, final IOException exception) {
                    context.complete();
                }
            });
        }
    }

    /** {@inheritDoc} */
    public synchronized Endpoint getValue() throws IllegalStateException {
        final Endpoint endpoint = this.endpoint;
        if (endpoint == null) throw new IllegalStateException();
        return endpoint;
    }

    public enum EndpointType {
        MANAGEMENT,
        SUBSYSTEM;
    }
}
