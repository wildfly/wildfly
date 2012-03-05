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
package org.jboss.as.remoting;

import java.net.BindException;
import java.net.InetSocketAddress;

import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

import static org.jboss.as.remoting.RemotingLogger.*;
import static org.jboss.as.remoting.RemotingMessages.*;

/**
 * Contains the remoting stream server
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractStreamServerService implements Service<AcceptingChannel<? extends ConnectedStreamChannel>>{

    private final Logger log = Logger.getLogger("org.jboss.as.remoting");

    @SuppressWarnings("rawtypes")
    private final InjectedValue<RemotingSecurityProvider> securityProviderValue = new InjectedValue<RemotingSecurityProvider>();
    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<SocketBindingManager> socketBindingManagerValue = new InjectedValue<SocketBindingManager>();
    private final OptionMap connectorPropertiesOptionMap;

    private volatile AcceptingChannel<? extends ConnectedStreamChannel> streamServer;
    private volatile ManagedBinding managedBinding;

    AbstractStreamServerService(final OptionMap connectorPropertiesOptionMap) {
        this.connectorPropertiesOptionMap = connectorPropertiesOptionMap;
    }

    @Override
    public AcceptingChannel<? extends ConnectedStreamChannel> getValue() throws IllegalStateException, IllegalArgumentException {
        return streamServer;
    }

    public InjectedValue<RemotingSecurityProvider> getSecurityProviderInjector() {
        return securityProviderValue;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointValue;
    }

    public InjectedValue<SocketBindingManager> getSocketBindingManagerInjector() {
        return socketBindingManagerValue;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        try {
            NetworkServerProvider networkServerProvider = endpointValue.getValue().getConnectionProviderInterface("remote", NetworkServerProvider.class);
            RemotingSecurityProvider rsp = securityProviderValue.getValue();
            ServerAuthenticationProvider sap = rsp.getServerAuthenticationProvider();
            OptionMap.Builder builder = OptionMap.builder();
            builder.addAll(rsp.getOptionMap());
            if (connectorPropertiesOptionMap != null) {
                builder.addAll(connectorPropertiesOptionMap);
            }
            streamServer = networkServerProvider.createServer(getSocketAddress(), builder.getMap(), sap, rsp.getXnioSsl());
            SocketBindingManager sbm = socketBindingManagerValue.getOptionalValue();
            if (sbm != null) {
                managedBinding = registerSocketBinding(sbm);
            }
            ROOT_LOGGER.listeningOnSocket(getSocketAddress());

        } catch (BindException e) {
            throw MESSAGES.couldNotBindToSocket(e.getMessage() + " " + getSocketAddress(), e);
        } catch (Exception e) {
            throw MESSAGES.couldNotStart(e);

        }
    }

    @Override
    public void stop(StopContext context) {
        IoUtils.safeClose(streamServer);
        SocketBindingManager sbm = socketBindingManagerValue.getOptionalValue();
        if (sbm != null && managedBinding != null) {
            unregisterSocketBinding(managedBinding, sbm);
        }
    }

    abstract InetSocketAddress getSocketAddress();

    abstract ManagedBinding registerSocketBinding(SocketBindingManager socketBindingManager);

    abstract void unregisterSocketBinding(ManagedBinding managedBinding, SocketBindingManager socketBindingManager);
}
