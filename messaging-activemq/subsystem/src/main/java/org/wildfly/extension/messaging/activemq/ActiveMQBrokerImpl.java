/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;

/**
 * Implementation of the wrapper over {@link org.apache.activemq.artemis.core.server.ActiveMQServer} and
 * {@link org.apache.activemq.artemis.core.server.management.ManagementService}.
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public class ActiveMQBrokerImpl implements ActiveMQBroker {
    private static final Object[] EMPTY_ARRAY = new Object[0];

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
        delegate.createQueue(address, routingType, queueName, filter, durable, temporary);
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
    public Object[] getResources(Class<?> resourceType) {
        if (delegate.getManagementService() != null) {
            return delegate.getManagementService().getResources(resourceType);
        }
        return EMPTY_ARRAY;
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
