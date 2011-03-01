/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.protocol.Connection;
import org.jboss.dmr.ModelNode;

/**
 * ProxyController implementation that connects to a remote ModelController via
 * the ModelController client protocol
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteProxyController implements ProxyController {

    private final ModelController delegate;
    private final PathAddress proxyNodeAddress;

    /**
     * Create a new model controller adapter connecting to a remote host
     *
     * @param address the address of the remote model controller to connect to
     * @param port the port of the remote model controller to connect to
     * @param proxyNodeAddress the address in the host ModelController where this proxy controller applies to
     */
    public static ProxyController create(final InetAddress inetAddress, final int port, final PathAddress proxyNodeAddress) throws UnknownHostException {
        if (inetAddress == null) {
            throw new IllegalArgumentException("Null address");
        }
        if (proxyNodeAddress == null) {
            throw new IllegalArgumentException("Null proxy address");
        }
        return new RemoteProxyController(new ModelControllerClientToModelControllerAdapter(inetAddress, port), proxyNodeAddress);
    }

    /**
     * Create a new model controller adapter reusing an existing connection
     *
     * @param connection the connection
     * @param proxyNodeAddress the address in the host ModelController where this proxy controller applies to
     */
    public static ProxyController create(final Connection connection, final PathAddress proxyNodeAddress) {
        if (connection == null) {
            throw new IllegalArgumentException("Null connection");
        }
        return new RemoteProxyController(new ModelControllerClientToModelControllerAdapter(connection), proxyNodeAddress);
    }

    private RemoteProxyController(ModelController delegate, PathAddress proxyNodeAddress) {
        this.delegate = delegate;
        this.proxyNodeAddress = proxyNodeAddress;
    }

    @Override
    public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
        return delegate.execute(executionContext, handler);
    }

    @Override
    public ModelNode execute(ExecutionContext executionContext) throws CancellationException {
        return delegate.execute(executionContext);
    }

    @Override
    public PathAddress getProxyNodeAddress() {
        return proxyNodeAddress;
    }
}
