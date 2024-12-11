/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms.legacy;

import jakarta.jms.ConnectionFactory;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

public class HornetQHelper {

    public static final LegacyConnectionFactory getLegacyConnectionFactory() {
        return ServiceLoader.load(LegacyConnectionFactory.class, HornetQHelper.class.getClassLoader()).findFirst().orElse(LEGACY_CONNECTION_FACTORY_INSTANCE);
    }

    private static final LegacyConnectionFactory LEGACY_CONNECTION_FACTORY_INSTANCE = new LegacyConnectionFactory() {
        @Override
        public ConnectionFactory createLegacyConnectionFactory(OperationContext context, ModelNode model) throws OperationFailedException {
            return null;
        }

        @Override
        public ConnectionFactory completeConnectionFactory(ActiveMQServer activeMQServer, ConnectionFactory uncompletedConnectionFactory, String discoveryGroupName, List<String> connectors) throws StartException {
            throw MessagingLogger.ROOT_LOGGER.noLegacyConnectionFactory();
        }

        @Override
        public void createQueue(ServiceTarget serviceTarget, String name, List<String> legacyEntries) {
            throw MessagingLogger.ROOT_LOGGER.noLegacyConnectionFactory();
        }

        @Override
        public void createTopic(ServiceTarget serviceTarget, String name, List<String> legacyEntries) {
            throw MessagingLogger.ROOT_LOGGER.noLegacyConnectionFactory();
        }
    };
}
