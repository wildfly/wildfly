/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.metadata;

import org.jboss.as.ee.logging.EeLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Metadata for configurations contained in jboss-ejb-client.xml descriptor
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJBClientDescriptorMetaData {

    private Boolean excludeLocalReceiver;
    private Boolean localReceiverPassByValue;
    private long invocationTimeout;
    private String deploymentNodeSelector;
    private String profile;
    private int defaultCompression = -1;

    private final Map<String, RemotingReceiverConfiguration> remotingReceiverConfigurations = new HashMap<String, RemotingReceiverConfiguration>();
    private final List<HttpConnectionConfiguration> httpConnectionConfigurations = new ArrayList<>();
    private final Set<ClusterConfig> clusterConfigs = new HashSet<ClusterConfig>();


    /**
     * Adds an outbound connection reference used by a remoting receiver in the client context represented
     * by this {@link EJBClientDescriptorMetaData}
     *
     * @param outboundConnectionRef The name of the outbound connection. Cannot be null or empty string.
     */
    public RemotingReceiverConfiguration addRemotingReceiverConnectionRef(final String outboundConnectionRef) {
        if (outboundConnectionRef == null || outboundConnectionRef.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.cannotAddRemotingReceiver();
        }
        final RemotingReceiverConfiguration remotingReceiverConfiguration = new RemotingReceiverConfiguration(outboundConnectionRef);
        this.remotingReceiverConfigurations.put(outboundConnectionRef, remotingReceiverConfiguration);
        return remotingReceiverConfiguration;
    }

    /**
     * Adds an HTTP connection
     * by this {@link EJBClientDescriptorMetaData}
     *
     * @param uri The uri of the HTTP outbound connection. Cannot be null or empty string.
     */
    public HttpConnectionConfiguration addHttpConnectionRef(final String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.cannotAddHTTPConnection();
        }
        final HttpConnectionConfiguration httpConnectionConfiguration = new HttpConnectionConfiguration(uri);
        this.httpConnectionConfigurations.add(httpConnectionConfiguration);
        return httpConnectionConfiguration;
    }

    /**
     * Returns a collection of outbound connection references that are used by the remoting receivers
     * configured in the client context of this {@link EJBClientDescriptorMetaData}
     *
     * @return
     */
    public Collection<RemotingReceiverConfiguration> getRemotingReceiverConfigurations() {
        return this.remotingReceiverConfigurations.values();
    }

    public List<HttpConnectionConfiguration> getHttpConnectionConfigurations() {
        return httpConnectionConfigurations;
    }

    /**
     * Set the pass-by-value semantics for the local receiver belonging to the Jakarta Enterprise Beans client context
     * represented by this metadata
     *
     * @param passByValue True if pass-by-value. False otherwise.
     */
    public void setLocalReceiverPassByValue(final Boolean passByValue) {
        this.localReceiverPassByValue = passByValue;
    }

    /**
     * If pass-by-value semantics for the local Jakarta Enterprise Beans receiver has been explicitly set, then returns that value.
     * Else returns null.
     *
     * @return
     */
    public Boolean isLocalReceiverPassByValue() {
        return this.localReceiverPassByValue;
    }

    /**
     * Exclude/include the local receiver in the Jakarta Enterprise Beans client context represented by this metadata.
     *
     * @param excludeLocalReceiver True if local receiver has to be excluded in the Jakarta Enterprise Beans client context. False otherwise.
     */
    public void setExcludeLocalReceiver(final Boolean excludeLocalReceiver) {
        this.excludeLocalReceiver = excludeLocalReceiver;
    }

    /**
     * Returns true if the local receiver is disabled in the Jakarta Enterprise Beans client context represented by this metadata.
     * Else returns false.
     *
     * @return
     */
    public Boolean isLocalReceiverExcluded() {
        return this.excludeLocalReceiver;
    }

    public Collection<ClusterConfig> getClusterConfigs() {
        return Collections.unmodifiableSet(this.clusterConfigs);
    }

    public ClusterConfig newClusterConfig(final String clusterName) {
        final ClusterConfig clusterConfig = new ClusterConfig(clusterName);
        this.clusterConfigs.add(clusterConfig);
        return clusterConfig;
    }

    public long getInvocationTimeout() {
        return this.invocationTimeout;
    }

    public void setInvocationTimeout(final long invocationTimeout) {
        this.invocationTimeout = invocationTimeout;
    }

    public String getDeploymentNodeSelector() {
        return this.deploymentNodeSelector;
    }

    public void setDeploymentNodeSelector(final String selector) {
        this.deploymentNodeSelector = selector;
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public int getDefaultCompression() {
        return defaultCompression;
    }

    public void setDefaultCompression(int defaultCompression) {
        this.defaultCompression = defaultCompression;
    }

    public class ClusterConfig extends CommonConnectionConfig {

        private final String clusterName;
        private final Set<ClusterNodeConfig> nodes = new HashSet<ClusterNodeConfig>();
        private long maxAllowedConnectedNodes;
        private String nodeSelector;


        ClusterConfig(final String clusterName) {
            this.clusterName = clusterName;
        }

        public ClusterNodeConfig newClusterNode(final String nodeName) {
            final ClusterNodeConfig node = new ClusterNodeConfig(nodeName);
            this.nodes.add(node);
            return node;
        }

        public Collection<ClusterNodeConfig> getClusterNodeConfigs() {
            return Collections.unmodifiableSet(this.nodes);
        }

        public String getClusterName() {
            return this.clusterName;
        }

        public long getMaxAllowedConnectedNodes() {
            return this.maxAllowedConnectedNodes;
        }

        public void setMaxAllowedConnectedNodes(final long maxAllowedConnectedNodes) {
            this.maxAllowedConnectedNodes = maxAllowedConnectedNodes;
        }

        public void setNodeSelector(final String nodeSelector) {
            this.nodeSelector = nodeSelector;
        }

        public String getNodeSelector() {
            return this.nodeSelector;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClusterConfig that = (ClusterConfig) o;

            if (!clusterName.equals(that.clusterName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return clusterName.hashCode();
        }
    }

    public class ClusterNodeConfig extends CommonConnectionConfig {

        private final String nodeName;

        ClusterNodeConfig(final String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeName() {
            return this.nodeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClusterNodeConfig that = (ClusterNodeConfig) o;

            if (!nodeName.equals(that.nodeName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return nodeName.hashCode();
        }
    }

    private class CommonConnectionConfig {
        private Properties connectionOptions;
        private Properties channelCreationOptions;
        private long connectTimeout;

        public void setConnectionOptions(final Properties connectionOptions) {
            this.connectionOptions = connectionOptions;
        }

        public void setChannelCreationOptions(final Properties channelCreationOptions) {
            this.channelCreationOptions = channelCreationOptions;
        }

        public long getConnectTimeout() {
            return this.connectTimeout;
        }

        public void setConnectTimeout(final long timeout) {
            this.connectTimeout = timeout;
        }

        public Properties getConnectionOptions() {
            return this.connectionOptions;
        }

        public Properties getChannelCreationOptions() {
            return this.channelCreationOptions;
        }
    }

    public class RemotingReceiverConfiguration {
        private final String outboundConnectionRef;

        private Properties channelCreationOptions;
        private long connectionTimeout;

        RemotingReceiverConfiguration(final String outboundConnectionRef) {
            this.outboundConnectionRef = outboundConnectionRef;
        }

        /**
         * Sets the channel creation options for this remoting receiver configuration
         *
         * @param channelCreationOpts The channel creation options
         */
        public void setChannelCreationOptions(final Properties channelCreationOpts) {
            this.channelCreationOptions = channelCreationOpts;
        }

        public Properties getChannelCreationOptions() {
            return this.channelCreationOptions;
        }

        public void setConnectionTimeout(final long timeout) {
            this.connectionTimeout = timeout;
        }

        public long getConnectionTimeout() {
            return this.connectionTimeout;
        }

        public String getOutboundConnectionRef() {
            return this.outboundConnectionRef;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RemotingReceiverConfiguration that = (RemotingReceiverConfiguration) o;

            if (outboundConnectionRef != null ? !outboundConnectionRef.equals(that.outboundConnectionRef) : that.outboundConnectionRef != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return outboundConnectionRef != null ? outboundConnectionRef.hashCode() : 0;
        }
    }

    public static final class HttpConnectionConfiguration {
        private final String uri;

        HttpConnectionConfiguration(final String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }
    }
}
