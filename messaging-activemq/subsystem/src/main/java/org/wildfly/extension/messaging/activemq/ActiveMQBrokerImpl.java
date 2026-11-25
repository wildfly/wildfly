/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.util.Collections;
import java.util.List;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;

/**
 * Implementation of the wrapper over {@link org.apache.activemq.artemis.core.server.ActiveMQServer} and
 * {@link org.apache.activemq.artemis.core.server.management.ManagementService}.
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public class ActiveMQBrokerImpl implements ActiveMQBroker {

    private final ActiveMQServer delegate;

    public ActiveMQBrokerImpl(org.apache.activemq.artemis.core.server.ActiveMQServer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    @Override
    public SimpleString getNodeID() {
        return delegate.getNodeID();
    }

    @Override
    public void addConnectorConfiguration(String string, TransportConfiguration tc) {
        delegate.getConfiguration().addConnectorConfiguration(string, tc);
    }

    @Override
    public void createQueue(SimpleString address, RoutingType routingType, SimpleString queueName, SimpleString filter, boolean durable, boolean temporary) throws Exception {
        QueueConfiguration config = QueueConfiguration.of(queueName).setAddress(address).setRoutingType(routingType).setFilterString(filter).setDurable(durable).setTemporary(temporary);
        delegate.createQueue(config);
    }

    @Override
    public void destroyQueue(SimpleString queueName, SecurityAuth session, boolean checkConsumerCount) throws Exception {
        delegate.destroyQueue(queueName, session, checkConsumerCount);
    }

    @Override
    public boolean isActive() {
        return delegate.isStarted() && delegate.isActive();
    }

    @Override
    public boolean hasResource(String resourceName) {
        return delegate.getManagementService() != null && delegate.getManagementService().getResource(resourceName) != null;
    }

    @Override
    public Object getResource(String resourceName) {
        if (delegate.getManagementService() != null) {
            return delegate.getManagementService().getResource(resourceName);
        }
        return null;
    }

    @Override
    public List<String> getCoreAddressNames() {
        if (delegate.getManagementService() != null) {
            return delegate.getManagementService().getAddressControlNames();
        }
        return Collections.emptyList();
    }

    @Override
    public List<QueueControl> getQueueControls() {
        if (delegate.getManagementService() != null) {
            return delegate.getManagementService().getQueueControls();
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getQueueControlNames() {
        if (delegate.getManagementService() != null) {
            return delegate.getManagementService().getQueueControlNames();
        }
        return Collections.emptyList();
    }

    @Override
    public ActiveMQServerControl getActiveMQServerControl() {
        return delegate.getActiveMQServerControl();
    }

    @Override
    public String getAddressSettingsAsJSON(String addressMatch) {
        AddressSettings settings = delegate.getAddressSettingsRepository().getMatch(addressMatch);
        settings.merge(delegate.getAddressSettingsRepository().getDefault());
        return ManagementUtil.convertAddressSettingInfosAsJSON(settings.toJSON());
    }

}
