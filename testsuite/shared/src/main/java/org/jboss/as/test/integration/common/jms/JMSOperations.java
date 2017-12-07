/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.common.jms;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import java.util.Map;

/**
 * Utility to administrate JMS-related resources on the server. An separate implementation should be created for
 * every possible JMS provider to be tested.
 * Use JMSOperationsProvider to get instances of implementing classes.
 *
 * Specify the fully qualified name of the activated implementation class in resources/jmsoperations.properties file.
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public interface JMSOperations {

    ModelControllerClient getControllerClient();

    ModelNode getServerAddress();

    String getProviderName();

    void createJmsQueue(String queueName, String jndiName);

    void createJmsQueue(String queueName, String jndiName, ModelNode attributes);

    void createJmsTopic(String topicName, String jndiName);

    void createJmsTopic(String topicName, String jndiName, ModelNode attributes);

    void removeJmsQueue(String queueName);

    void removeJmsTopic(String topicName);

    void addJmsConnectionFactory(String name, String jndiName, ModelNode attributes);

    void removeJmsConnectionFactory(String name);

    void addJmsBridge(String name, ModelNode attributes);

    void removeJmsBridge(String name);

    void addCoreQueue(String queueName, String queueAddress, boolean durable);

    void removeCoreQueue(String queueName);

    /**
     * Creates remote acceptor
     *
     * @param name          name of the remote acceptor
     * @param socketBinding name of socket binding
     * @param params        params
     */
    void createRemoteAcceptor(String name, String socketBinding, Map<String, String> params);

    /**
     * Remove remote acceptor
     *
     * @param name          name of the remote acceptor
     */
    void removeRemoteAcceptor(String name);

    void close();

    void addHttpConnector(String connectorName, String socketBinding, String endpoint);

    void removeHttpConnector(String connectorName);

    /**
     * Set system properties for the given destination and resourceAdapter.
     *
     * The system property for the given destination is {@code destination} and the one for the resourceAdapter is {@code resource.adapter}.
     */
    void setSystemProperties(String destination, String resourceAdapter);

    void removeSystemProperties();
}
