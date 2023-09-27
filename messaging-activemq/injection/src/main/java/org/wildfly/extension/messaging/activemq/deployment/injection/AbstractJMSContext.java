/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment.injection;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

/**
 * Abstract class for managing JMS Contexts.
 *
 * 2 subclasses are provided with different CDI scope (@RequestScoped and @TransactionScoped).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public abstract class AbstractJMSContext implements Serializable {

    private final Map<String, JMSContext> contexts = new ConcurrentHashMap<>();

    JMSContext getContext(String injectionPointId, JMSInfo info, ConnectionFactory connectionFactory) {
        return contexts.computeIfAbsent(injectionPointId, key -> {
            return createContext(info, connectionFactory);
        });
    }

    private JMSContext createContext(JMSInfo info, ConnectionFactory connectionFactory) {
        ROOT_LOGGER.debugf("Create JMSContext from %s - %s", info, connectionFactory);
        int sessionMode = info.getSessionMode();
        String userName = info.getUserName();
        final JMSContext context;
        if (userName == null) {
            context = connectionFactory.createContext(sessionMode);
        } else {
            String password = info.getPassword();
            context = connectionFactory.createContext(userName, password, sessionMode);
        }
        return context;
    }

    void cleanUp() {
        ROOT_LOGGER.debugf("Clean up JMSContext created from %s", this);
        for (JMSContext jmsContext : contexts.values()) {
            jmsContext.close();
        }
        contexts.clear();
    }
}
