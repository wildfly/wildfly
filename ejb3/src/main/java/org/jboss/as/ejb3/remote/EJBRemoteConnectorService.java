/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.remoting.RemotingConnectorBindingInfoService;
import org.jboss.ejb.protocol.remote.RemoteEJBService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service<EJBRemoteConnectorService> {

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();
    private final InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> remotingConnectorInfoInjectedValue = new InjectedValue<>();
    private final InjectedValue<AssociationService> associationServiceInjectedValue = new InjectedValue<>();
    private final InjectedValue<RemotingTransactionService> remotingTransactionServiceInjectedValue = new InjectedValue<>();
    private final InjectedValue<ControlledProcessStateService> controlledProcessStateServiceInjectedValue = new InjectedValue<>();
    private volatile Registration registration;
    private final OptionMap channelCreationOptions;

    public EJBRemoteConnectorService() {
        this(OptionMap.EMPTY);
    }

    public EJBRemoteConnectorService(final OptionMap channelCreationOptions) {
        this.channelCreationOptions = channelCreationOptions;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final AssociationService associationService = associationServiceInjectedValue.getValue();
        final Endpoint endpoint = endpointValue.getValue();
        RemoteEJBService remoteEJBService = RemoteEJBService.create(
            associationService.getAssociation(),
            remotingTransactionServiceInjectedValue.getValue()
        );
        final ControlledProcessStateService processStateService = controlledProcessStateServiceInjectedValue.getValue();
        if (processStateService.getCurrentState() == ControlledProcessState.State.STARTING) {
            final PropertyChangeListener listener = new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("currentState") && evt.getOldValue() == ControlledProcessState.State.STARTING) {
                        remoteEJBService.serverUp();
                        // can't use a lambda because of this line...
                        processStateService.removePropertyChangeListener(this);
                    }
                }
            };
            processStateService.addPropertyChangeListener(listener);
            // this is actually racy, so we have to double-check the state afterwards just to be sure it didn't transition before we got here.
            if (processStateService.getCurrentState() != ControlledProcessState.State.STARTING) {
                // this method is idempotent so it's OK if the listener got fired
                remoteEJBService.serverUp();
                // this one too
                processStateService.removePropertyChangeListener(listener);
            }
        } else {
            remoteEJBService.serverUp();
        }

        // Register an EJB channel open listener
        OpenListener channelOpenListener = remoteEJBService.getOpenListener();
        try {
            registration = endpoint.registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        registration.close();
    }

    public String getProtocol() {
        return remotingConnectorInfoInjectedValue.getValue().getProtocol();
    }

    @Override
    public EJBRemoteConnectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointValue;
    }

    public InjectedValue<RemotingConnectorBindingInfoService.RemotingConnectorInfo> getRemotingConnectorInfoInjectedValue() {
        return remotingConnectorInfoInjectedValue;
    }

    public InjectedValue<RemotingTransactionService> getRemotingTransactionServiceInjector() {
        return remotingTransactionServiceInjectedValue;
    }

    public InjectedValue<ControlledProcessStateService> getControlledProcessStateServiceInjector() {
        return controlledProcessStateServiceInjectedValue;
    }

    public InjectedValue<AssociationService> getAssociationServiceInjector() {
        return associationServiceInjectedValue;
    }
}
