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

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.ClusterNodeSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceRegistry;
import org.xnio.OptionMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jaikiran Pai
 */
public class EJBClientClusterConfig extends EJBClientCommonConnectionConfig implements EJBClientConfiguration.ClusterConfiguration {

    private static final Logger logger = Logger.getLogger(EJBClientClusterConfig.class);

    private final EJBClientDescriptorMetaData.ClusterConfig delegate;
    private final Map<String, EJBClientConfiguration.ClusterNodeConfiguration> nodes = new HashMap<String, EJBClientConfiguration.ClusterNodeConfiguration>();
    private final ClusterNodeSelector clusterNodeSelector;

    public EJBClientClusterConfig(final EJBClientDescriptorMetaData.ClusterConfig clusterConfig, final ClassLoader deploymentClassLoader, final ServiceRegistry serviceRegistry) {
        this.delegate = clusterConfig;
        this.setConnectionTimeout(clusterConfig.getConnectTimeout());
        // setup the channel creation options
        final Properties channelProps = clusterConfig.getChannelCreationOptions();
        if (channelProps != null) {
            // we don't use the deployment CL here since the XNIO project isn't necessarily added as a dep on the deployment's
            // module CL
            final OptionMap channelCreationOptions = getOptionMapFromProperties(channelProps, this.getClass().getClassLoader());
            logger.debug("Channel creation options for cluster " + clusterConfig.getClusterName() + " are " + channelCreationOptions);
            this.setChannelCreationOptions(channelCreationOptions);
        }

        // setup connection creation options
        final Properties connectionProps = clusterConfig.getConnectionOptions();
        if (connectionProps != null) {
            // we don't use the deployment CL here since the XNIO project isn't necessarily added as a dep on the deployment's
            // module CL
            final OptionMap connectionCreationOptions = getOptionMapFromProperties(connectionProps, this.getClass().getClassLoader());
            logger.debug("Connection creation options for cluster " + clusterConfig.getClusterName() + " are " + connectionCreationOptions);
            this.setConnectionCreationOptions(connectionCreationOptions);
        }

        this.setCallbackHandler(serviceRegistry, clusterConfig.getUserName(), clusterConfig.getSecurityRealm());

        final String nodeSelector = clusterConfig.getNodeSelector();
        if (nodeSelector != null) {
            try {
                final Class<?> nodeSelectorClass = deploymentClassLoader.loadClass(nodeSelector);
                this.clusterNodeSelector = (ClusterNodeSelector) nodeSelectorClass.newInstance();
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failureDuringLoadOfClusterNodeSelector(nodeSelector, clusterConfig.getClusterName(), e);
            }
        } else {
            this.clusterNodeSelector = null;
        }
    }

    @Override
    public String getClusterName() {
        return this.delegate.getClusterName();
    }

    @Override
    public long getMaximumAllowedConnectedNodes() {
        return this.delegate.getMaxAllowedConnectedNodes();
    }

    @Override
    public ClusterNodeSelector getClusterNodeSelector() {
        return this.clusterNodeSelector;
    }

    @Override
    public EJBClientConfiguration.ClusterNodeConfiguration getNodeConfiguration(String nodeName) {
        return this.nodes.get(nodeName);
    }

    public void addClusterNode(final EJBClientConfiguration.ClusterNodeConfiguration node) {
        this.nodes.put(node.getNodeName(), node);
    }

    @Override
    public boolean isConnectEagerly() {
        return true;
    }
}
