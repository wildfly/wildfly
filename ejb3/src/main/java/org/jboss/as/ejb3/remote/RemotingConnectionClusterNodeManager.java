/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.ejb.client.ClusterContext;
import org.jboss.ejb.client.ClusterNodeManager;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.ejb.client.remoting.NetworkUtil;
import org.jboss.ejb.client.remoting.ReconnectHandler;
import org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jaikiran Pai
 */
class RemotingConnectionClusterNodeManager implements ClusterNodeManager {

    private static final Logger logger = Logger.getLogger(RemotingConnectionClusterNodeManager.class);

    private final String nodeName;
    private final ClusterContext clusterContext;
    private final String destinationHost;
    private final int destinationPort;
    private final String destinationProtocol;
    private final Endpoint endpoint;


    RemotingConnectionClusterNodeManager(final ClusterContext clusterContext, final Endpoint endpoint,
                                         final String nodeName, final String destinationHost,
                                         final int destinationPort, String destinationProtocol) {
        this.nodeName = nodeName;
        this.clusterContext = clusterContext;
        this.destinationHost = destinationHost;
        this.destinationPort = destinationPort;
        this.endpoint = endpoint;
        this.destinationProtocol = destinationProtocol;
    }

    @Override
    public String getNodeName() {
        return this.nodeName;
    }

    @Override
    public EJBReceiver getEJBReceiver() {
        Connection connection;
        final ReconnectHandler reconnectHandler;
        OptionMap channelCreationOptions = OptionMap.EMPTY;
        final EJBClientConfiguration ejbClientConfiguration = this.clusterContext.getEJBClientContext().getEJBClientConfiguration();
        try {
            // if the client configuration is available create the connection using those configs
            if (ejbClientConfiguration != null) {
                final EJBClientConfiguration.ClusterConfiguration clusterConfiguration = ejbClientConfiguration.getClusterConfiguration(clusterContext.getClusterName());
                if (clusterConfiguration == null) {
                    // use default configurations
                    final OptionMap connectionCreationOptions = OptionMap.EMPTY;
                    final CallbackHandler callbackHandler = ejbClientConfiguration.getCallbackHandler();
                    final IoFuture<Connection> futureConnection = NetworkUtil.connect(endpoint, destinationProtocol, destinationHost, destinationPort, null, connectionCreationOptions, callbackHandler, null);
                    // wait for the connection to be established
                    connection = IoFutureHelper.get(futureConnection, 5000, TimeUnit.MILLISECONDS);
                    // create a re-connect handler (which will be used on connection breaking down)
                    reconnectHandler = new ClusterNodeReconnectHandler(destinationHost, destinationPort, connectionCreationOptions, callbackHandler, channelCreationOptions, 5000);

                } else {
                    final EJBClientConfiguration.ClusterNodeConfiguration clusterNodeConfiguration = clusterConfiguration.getNodeConfiguration(this.getNodeName());
                    // use the specified configurations
                    channelCreationOptions = clusterNodeConfiguration == null ? clusterConfiguration.getChannelCreationOptions() : clusterNodeConfiguration.getChannelCreationOptions();
                    final OptionMap connectionCreationOptions = clusterNodeConfiguration == null ? clusterConfiguration.getConnectionCreationOptions() : clusterNodeConfiguration.getConnectionCreationOptions();
                    final CallbackHandler callbackHandler = clusterNodeConfiguration == null ? clusterConfiguration.getCallbackHandler() : clusterNodeConfiguration.getCallbackHandler();
                    final IoFuture<Connection> futureConnection = NetworkUtil.connect(endpoint, destinationProtocol, destinationHost, destinationPort, null, connectionCreationOptions, callbackHandler, null);
                    final long timeout = clusterNodeConfiguration == null ? clusterConfiguration.getConnectionTimeout() : clusterNodeConfiguration.getConnectionTimeout();
                    // wait for the connection to be established
                    connection = IoFutureHelper.get(futureConnection, timeout, TimeUnit.MILLISECONDS);
                    // create a re-connect handler (which will be used on connection breaking down)
                    reconnectHandler = new ClusterNodeReconnectHandler(destinationHost, destinationPort, connectionCreationOptions, callbackHandler, channelCreationOptions, timeout);
                }

            } else {
                // create the connection using defaults
                final OptionMap connectionCreationOptions = OptionMap.EMPTY;
                final CallbackHandler callbackHandler = new AnonymousCallbackHandler();
                final IoFuture<Connection> futureConnection = NetworkUtil.connect(endpoint,destinationProtocol, destinationHost, destinationPort, null, connectionCreationOptions, callbackHandler, null);
                // wait for the connection to be established
                connection = IoFutureHelper.get(futureConnection, 5000, TimeUnit.MILLISECONDS);
                // create a re-connect handler (which will be used on connection breaking down)
                reconnectHandler = new ClusterNodeReconnectHandler(destinationHost, destinationPort, connectionCreationOptions, callbackHandler, channelCreationOptions, 5000);

            }
        } catch (Exception e) {
            logger.info("Could not create a connection for cluster node " + this.nodeName + " in cluster " + clusterContext.getClusterName(), e);
            return null;
        }
        return new RemotingConnectionEJBReceiver(connection, reconnectHandler, channelCreationOptions, destinationProtocol);
    }


    private class ClusterNodeReconnectHandler implements ReconnectHandler {

        private final String destinationHost;
        private final int destinationPort;
        private final OptionMap connectionCreationOptions;
        private final OptionMap channelCreationOptions;
        private final CallbackHandler callbackHandler;
        private final long connectionTimeout;

        ClusterNodeReconnectHandler(final String host, final int port, final OptionMap connectionCreationOptions, final CallbackHandler callbackHandler, final OptionMap channelCreationOptions, final long connectionTimeoutInMillis) {
            this.destinationHost = host;
            this.destinationPort = port;
            this.connectionCreationOptions = connectionCreationOptions;
            this.channelCreationOptions = channelCreationOptions;
            this.callbackHandler = callbackHandler;
            this.connectionTimeout = connectionTimeoutInMillis;
        }

        @Override
        public void reconnect() throws IOException {
            Connection connection = null;
            try {
                final IoFuture<Connection> futureConnection = NetworkUtil.connect(endpoint,"remote", destinationHost, destinationPort, null, connectionCreationOptions, callbackHandler, null);
                connection = IoFutureHelper.get(futureConnection, connectionTimeout, TimeUnit.MILLISECONDS);
                logger.debugf("Successfully reconnected to connection %s", connection);

            } catch (Exception e) {
                logger.debugf(e, "Failed to re-connect to %s:%d", this.destinationHost, this.destinationPort);
            }
            if (connection == null) {
                return;
            }
            try {
                final EJBReceiver ejbReceiver = new RemotingConnectionEJBReceiver(connection, this, channelCreationOptions, "remote");
                RemotingConnectionClusterNodeManager.this.clusterContext.registerEJBReceiver(ejbReceiver);
            } finally {
                // if we successfully re-connected then unregister this ReconnectHandler from the EJBClientContext
                RemotingConnectionClusterNodeManager.this.clusterContext.getEJBClientContext().unregisterReconnectHandler(this);
            }

        }
    }
}
