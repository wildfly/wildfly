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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.xnio.ChannelListener;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.channels.ConnectedStreamChannel;

/**
 * Add a connector to a remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public final class AddConnectorUpdate extends AbstractRemotingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -1238913680118311381L;

    private final String name;
    private final String socketBinding;
    private SaslElement saslElement;
    private String authenticationProvider;
    private final Map<String, String> properties = new HashMap<String, String>();

    public AddConnectorUpdate(String name, String socketBinding) {
        this.name = name;
        this.socketBinding = socketBinding;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
        final OptionMap.Builder builder = OptionMap.builder();

        // First, apply options to option map.
        if (saslElement != null) saslElement.applyTo(builder);
        // TODO apply connector properties to option map

        // Create the service.
        final ConnectorService connectorService = new ConnectorService();
        connectorService.setOptionMap(builder.getMap());

        // The update handler
        final UpdateResultHandler.ServiceStartListener<P> listener = new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param);

        // Register the service with the container and inject dependencies.
        final ServiceName connectorName = ConnectorElement.connectorName(name);
        final BatchServiceBuilder<ChannelListener<ConnectedStreamChannel<InetSocketAddress>>> serviceBuilder = batchBuilder.addService(connectorName, connectorService);
        serviceBuilder.addDependency(connectorName.append("auth-provider"), ServerAuthenticationProvider.class, connectorService.getAuthenticationProviderInjector());
        serviceBuilder.addDependency(RemotingSubsystemElement.JBOSS_REMOTING_ENDPOINT, Endpoint.class, connectorService.getEndpointInjector());
        serviceBuilder.addListener(listener);
        serviceBuilder.setInitialMode(ServiceController.Mode.IMMEDIATE);

        // TODO create XNIO connector service from socket-binding, with dependency on connectorName
        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            resultHandler.handleFailure(e, param);
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<RemotingSubsystemElement, ?> getCompensatingUpdate(RemotingSubsystemElement original) {
        return new RemoveConnectorUpdate(name);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(RemotingSubsystemElement element) throws UpdateFailedException {
        final ConnectorElement connector = element.addConnector(name, socketBinding);
        connector.setAuthenticationProvider(authenticationProvider);
        connector.setSaslElement(saslElement);
        connector.setConnectorProperties(properties);
    }

    public void setSaslElement(SaslElement saslElement) {
        this.saslElement = saslElement;
    }

    public void setAuthenticationProvider(String authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
