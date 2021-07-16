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

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.as.network.ProtocolSocketBinding;
import org.jboss.ejb.protocol.remote.RemoteEJBService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.wildfly.transaction.client.provider.remoting.RemotingTransactionService;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class EJBRemoteConnectorService implements Service<Void> {

    // TODO: Should this be exposed via the management APIs?
    private static final String EJB_CHANNEL_NAME = "jboss.ejb";

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<>();
    private final InjectedValue<AssociationService> associationServiceInjectedValue = new InjectedValue<>();
    private final InjectedValue<RemotingTransactionService> remotingTransactionServiceInjectedValue = new InjectedValue<>();
    private final List<InjectedValue<ProtocolSocketBinding>> connectorsProtocolSocketBindings = new LinkedList<>();
    private volatile Registration registration;
    private final OptionMap channelCreationOptions;
    private final Function<String, Boolean> classResolverFilter;
    private Set<Integer> connectorsPortSet = new HashSet<Integer>();

    public EJBRemoteConnectorService(final OptionMap channelCreationOptions,
                                     final Function<String, Boolean> classResolverFilter) {
        this.channelCreationOptions = channelCreationOptions;
        this.classResolverFilter = classResolverFilter;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final AssociationService associationService = associationServiceInjectedValue.getValue();
        final Endpoint endpoint = endpointValue.getValue();
        Executor executor = executorService.getOptionalValue();
        if (executor != null) {
            associationService.setExecutor(executor);
        }
        RemoteEJBService remoteEJBService = RemoteEJBService.create(
            associationService.getAssociation(),
            remotingTransactionServiceInjectedValue.getValue(),
            classResolverFilter
        );
        remoteEJBService.serverUp();

        // initialise the set of ports which are listed in the connectors attribute of <remote/>
        for (InjectedValue<ProtocolSocketBinding> bindingInjectedValue : connectorsProtocolSocketBindings) {
            int port = bindingInjectedValue.getValue().getSocketBinding().getPort();
            connectorsPortSet.add(new Integer(port));
        }

        // set up a predicate which prohibits "jboss.ejb" services to be created on connectors *not* listed in the <remote/> resource
        Predicate<Connection> channelConnectorPredicate = (connection) -> {
            int port = ((InetSocketAddress)(connection.getLocalAddress())).getPort();
            return connectorsPortSet.contains(new Integer(port));
        };

        // Register an EJB channel open listener which uses
        OpenListener channelOpenListener = remoteEJBService.getOpenListener();
        try {
            registration = endpoint.registerService(EJB_CHANNEL_NAME, channelOpenListener, this.channelCreationOptions, channelConnectorPredicate);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        final AssociationService associationService = associationServiceInjectedValue.getValue();
        associationService.sendTopologyUpdateIfLastNodeToLeave();
        associationService.setExecutor(null);
        registration.close();
    }

    @Override
    public Void getValue() {
        return null;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointValue;
    }

    public InjectedValue<RemotingTransactionService> getRemotingTransactionServiceInjector() {
        return remotingTransactionServiceInjectedValue;
    }

    public InjectedValue<AssociationService> getAssociationServiceInjector() {
        return associationServiceInjectedValue;
    }

    public InjectedValue<ExecutorService> getExecutorService() {
        return executorService;
    }

    public InjectedValue<ProtocolSocketBinding> addConnectorInjector(String connectorName) {
        InjectedValue<ProtocolSocketBinding> info = new InjectedValue<>();
        this.connectorsProtocolSocketBindings.add(info);
        return info;
    }
}
