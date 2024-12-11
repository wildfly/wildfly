/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms.legacy;

import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;

public interface LegacyConnectionFactory {

    ConnectionFactory createLegacyConnectionFactory(OperationContext context, ModelNode model) throws OperationFailedException;

    ConnectionFactory completeConnectionFactory(ActiveMQServer activeMQServer, ConnectionFactory uncompletedConnectionFactory, String discoveryGroupName, List<String> connectors) throws StartException;

    void createQueue(ServiceTarget serviceTarget, String name, List<String> legacyEntries);

    void createTopic(ServiceTarget serviceTarget, String name, List<String> legacyEntries);
}
