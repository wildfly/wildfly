/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms.legacy;


import static org.wildfly.extension.messaging.activemq.CommonAttributes.LEGACY;

import java.util.List;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;
import org.wildfly.extension.messaging.activemq.ActiveMQBroker;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryService implements Service<ConnectionFactory> {

    private final InjectedValue<ActiveMQBroker> injectedActiveMQServer = new InjectedValue<ActiveMQBroker>();
    private final ConnectionFactory uncompletedConnectionFactory;
    private final String discoveryGroupName;
    private final List<String> connectors;
    private final LegacyConnectionFactory factory;

    private ConnectionFactory connectionFactory;

    public LegacyConnectionFactoryService(LegacyConnectionFactory factory, ConnectionFactory uncompletedConnectionFactory, String discoveryGroupName, List<String> connectors) {
        this.factory = factory;
        this.uncompletedConnectionFactory = uncompletedConnectionFactory;
        this.discoveryGroupName = discoveryGroupName;
        this.connectors = connectors;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ActiveMQServer activeMQServer = ActiveMQServer.class.cast(injectedActiveMQServer.getValue().getDelegate());
        connectionFactory = factory.completeConnectionFactory(activeMQServer, uncompletedConnectionFactory, discoveryGroupName, connectors);
    }

    @Override
    public void stop(StopContext context) {
        connectionFactory = null;
    }

    @Override
    public ConnectionFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return connectionFactory;
    }

    public static LegacyConnectionFactoryService installService(final String name,
            final ServiceName activeMQServerServiceName,
            final ServiceTarget serviceTarget,
            final LegacyConnectionFactory factory,
            final ConnectionFactory uncompletedConnectionFactory,
            final String discoveryGroupName,
            final List<String> connectors) {
        final LegacyConnectionFactoryService service = new LegacyConnectionFactoryService(factory, uncompletedConnectionFactory, discoveryGroupName, connectors);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(activeMQServerServiceName).append(LEGACY, name);

        final ServiceBuilder sb = serviceTarget.addService(serviceName, service);
        sb.requires(ActiveMQActivationService.getServiceName(activeMQServerServiceName));
        sb.addDependency(activeMQServerServiceName, ActiveMQBroker.class, service.injectedActiveMQServer);
        sb.setInitialMode(ServiceController.Mode.PASSIVE);
        sb.install();
        return service;
    }

}
