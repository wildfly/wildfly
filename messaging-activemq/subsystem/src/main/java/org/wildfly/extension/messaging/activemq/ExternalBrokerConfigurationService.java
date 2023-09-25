/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.util.Map;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalBrokerConfigurationService implements Service<ExternalBrokerConfigurationService> {
    private final Map<String, TransportConfiguration> connectors;
    private final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations;
    private final Map<String, ServiceName> socketBindings;
    private final Map<String, ServiceName> outboundSocketBindings;
    private final Map<String, ServiceName> groupBindings;
    // mapping between the {broadcast|discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames;
    // mapping between the {broadcast|discovery}-groups and the command dispatcher factory they use
    private final Map<String, ServiceName> commandDispatcherFactories;
    private final Map<String, String> sslContextNames;

    public ExternalBrokerConfigurationService(final Map<String, TransportConfiguration> connectors,
            Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations,
            Map<String, ServiceName> socketBindings,
            Map<String, ServiceName> outboundSocketBindings,
            Map<String, ServiceName> groupBindings,
            Map<String, ServiceName> commandDispatcherFactories,
            Map<String, String> clusterNames,
            Map<String, String> sslContextNames) {
        this.connectors = connectors;
        this.discoveryGroupConfigurations = discoveryGroupConfigurations;
        this.clusterNames = clusterNames;
        this.commandDispatcherFactories = commandDispatcherFactories;
        this.groupBindings = groupBindings;
        this.outboundSocketBindings = outboundSocketBindings;
        this.socketBindings = socketBindings;
        this.sslContextNames = sslContextNames;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    public Map<String, TransportConfiguration> getConnectors() {
        return connectors;
    }

    public Map<String, ServiceName> getSocketBindings() {
        return socketBindings;
    }

    public Map<String, ServiceName> getOutboundSocketBindings() {
        return outboundSocketBindings;
    }

    public Map<String, ServiceName> getGroupBindings() {
        return groupBindings;
    }

    public Map<String, String> getClusterNames() {
        return clusterNames;
    }

    public Map<String, ServiceName> getCommandDispatcherFactories() {
        return commandDispatcherFactories;
    }

    public Map<String, DiscoveryGroupConfiguration> getDiscoveryGroupConfigurations() {
        return discoveryGroupConfigurations;
    }

    public Map<String, String> getSslContextNames() {
        return sslContextNames;
    }

    @Override
    public ExternalBrokerConfigurationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
