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

package org.jboss.as.ejb3.remote.protocol.versiontwo;

import org.jboss.as.clustering.registry.RegistryCollector;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.RemoteAsyncInvocationCancelStatusService;
import org.jboss.as.ejb3.remote.protocol.MessageHandler;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.as.ejb3.remote.protocol.versionone.VersionOneProtocolChannelReceiver;
import org.jboss.as.network.ClientMapping;
import org.jboss.marshalling.MarshallerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Jaikiran Pai
 */
public class VersionTwoProtocolChannelReceiver extends VersionOneProtocolChannelReceiver {

    private static final byte HEADER_TX_RECOVER_MESSAGE = 0x19;

    public VersionTwoProtocolChannelReceiver(final ChannelAssociation channelAssociation, final DeploymentRepository deploymentRepository,
                                             final EJBRemoteTransactionsRepository transactionsRepository, final RegistryCollector<String, List<ClientMapping>> clientMappingRegistryCollector,
                                             final MarshallerFactory marshallerFactory, final ExecutorService executorService, final RemoteAsyncInvocationCancelStatusService asyncInvocationCancelStatusService) {
        super(channelAssociation, deploymentRepository, transactionsRepository, clientMappingRegistryCollector, marshallerFactory, executorService, asyncInvocationCancelStatusService);
    }


    @Override
    protected MessageHandler getMessageHandler(byte header) {
        switch (header) {
            case HEADER_TX_RECOVER_MESSAGE:
                return new TransactionRecoverMessageHandler(this.transactionsRepository, this.marshallerFactory, this.executorService);
            default:
                return super.getMessageHandler(header);
        }
    }
}
