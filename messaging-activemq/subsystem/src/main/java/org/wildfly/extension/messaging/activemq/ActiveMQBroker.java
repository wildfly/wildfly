/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.List;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.security.SecurityAuth;

/**
 * Wrapper interface to expose methods from {@link org.apache.activemq.artemis.core.server.ActiveMQServer}
 * and {@link org.apache.activemq.artemis.core.server.management.ManagementService} that are needed by a
 * {@code /subsystem=messaging-activemq/server=*} resource or its children.
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public interface ActiveMQBroker {

    /**
     * To get an untyped access to {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @return the underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}
     */
    Object getDelegate();

    /**
     * @return the server node id.
     */
    SimpleString getNodeID();

    /**
     * Add a connector configuration to the current {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param string the name of the connector.
     * @param tc the transport configuration.
     */
    void addConnectorConfiguration(String string, TransportConfiguration tc);

    /**
     * Creates a queue on the underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param address the queue address.
     * @param routingType the queue routing type (anycast or multicast).
     * @param queueName the name of the queue.
     * @param filter the optional message filter; may be {@code null}.
     * @param durable whether the queue should survive server restarts.
     * @param temporary whether the queue is temporary and should be deleted when its creating connection closes.
     * @throws Exception if the queue cannot be created.
     */
    void createQueue(SimpleString address, RoutingType routingType, SimpleString queueName, SimpleString filter,
                     boolean durable, boolean temporary) throws Exception;

    /**
     * Destroys a queue from the underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param queueName the name of the queue to destroy.
     * @param session the security context used to authorize the operation; may be {@code null}.
     * @param checkConsumerCount whether to reject the operation if the queue still has active consumers.
     * @throws Exception if the queue cannot be destroyed.
     */
    void destroyQueue(SimpleString queueName, SecurityAuth session, boolean checkConsumerCount) throws Exception;

    /**
     * Returns {@code true} if the server is started and active, {@code false} otherwise.
     * @return {@code true} if the server is started and active, {@code false} otherwise.
     */
    boolean isActive();

    /**
     * Returns {@code true} if a management resource with the given name exists, {@code false} otherwise.
     * @param resourceName the name of the resource.
     * @return {@code true} if the resource exists, {@code false} otherwise.
     * @see org.apache.activemq.artemis.core.server.management.ManagementService#getResource(String)
     */
    boolean hasResource(String resourceName);

    /**
     * Returns the management resource registered under the given name, or {@code null} if none exists.
     * @param resourceName the name of the resource.
     * @return the management resource, or {@code null} if none exists.
     * @see org.apache.activemq.artemis.core.server.management.ManagementService#getResource(String)
     */
    Object getResource(String resourceName);

    /**
     * Returns the names of all core addresses known to the underlying
     * {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @return the list of core address names; never {@code null}.
     */
    List<String> getCoreAddressNames();

    /**
     * Returns the {@link QueueControl} instances for all queues on the underlying
     * {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @return the list of queue controls; never {@code null}.
     */
    List<QueueControl> getQueueControls();

    /**
     * Returns the names of all queues on the underlying
     * {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @return the set of queue names; never {@code null}.
     */
    List<String> getQueueControlNames();

    /**
     * Returns the {@link org.apache.activemq.artemis.api.core.management.ActiveMQServerControl} to manage the
     * underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @return the {@link org.apache.activemq.artemis.api.core.management.ActiveMQServerControl}
     */
    ActiveMQServerControl getActiveMQServerControl();

    /**
     * Returns the JSON description of the address settings of the specified match address.
     * @param addressMatch the address settings match address.
     * @return the JSON description of the address settings of the specified match address
     */
    String getAddressSettingsAsJSON(String addressMatch);
}
