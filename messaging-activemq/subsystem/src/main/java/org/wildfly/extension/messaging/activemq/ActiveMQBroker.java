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

/**
 * Wrapper interface to expose methods from {@link org.apache.activemq.artemis.core.server.ActiveMQServer}
 * and {@link org.apache.activemq.artemis.core.server.management.ManagementService} that are needed by a
 * {@code /subsystem=messaging-activemq/server=*} resource or its children."

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
     * Add a connection configuration to the current {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param string: the name of the connector.
     * @param tc: the trnasport configurartion.
     */
    void addConnectorConfiguration(String string, TransportConfiguration tc);

    /**
     * Creates a Queue on the underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param address: the queue address.
     * @param routingType: the queue routing type (anycast or mulicast).
     * @param queueName: the name of the queue.
     * @param filter: the filter.
     * @param durable
     * @param temporary
     * @throws Exception
     */
    void  createQueue(SimpleString address, RoutingType routingType, SimpleString queueName, SimpleString filter,
                     boolean durable, boolean temporary) throws Exception;

    /**
     * Destroys a queue from the underlying {@link org.apache.activemq.artemis.core.server.ActiveMQServer}.
     * @param queueName
     * @param session
     * @param checkConsumerCount
     * @throws Exception
     */
    void destroyQueue(SimpleString queueName, SecurityAuth session, boolean checkConsumerCount) throws Exception;

    /**
     * Returns the current state of the server: true - if the server is started and active - false otherwise.
     * @return the current state of the server: true - if the server is started and active - false otherwise.
     */
    boolean isActive();

    /**
     * Returns true if the resource exists - false otherwise.
     * @param resourceName: the name of the reosurce.
     * @return true if the resource exists - false otherwise.
     * @see org.apache.activemq.artemis.core.server.management.ManagementService#getResource(java.lang.String).
     */
    boolean hasResource(String resourceName);

    /**
     * Returns the untyped resource with the specified name- null if none exists.
     * @param resourceName: the name of the resource.
     * @return the untyped resource with the specified name- null if none exists.
     * @see org.apache.activemq.artemis.core.server.management.ManagementService#getResource(java.lang.String).
     */
    Object getResource(String resourceName);

    /**
     * Returns an untyped array of the resources of this type - an empty arry if none exists.
     * @param resourceType: the type of resources.
     * @return an untyped array of the resources of this type - an empty arry if none exists.
     * @see org.apache.activemq.artemis.core.server.management.ManagementService#getResources(java.lang.Class).
     */
    Object[] getResources(Class<?> resourceType);

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
