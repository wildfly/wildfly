/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.injection.deployment;

import static org.wildfly.extension.messaging.activemq.injection._private.MessagingLogger.ROOT_LOGGER;

import java.io.Serializable;
import java.util.Map;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract class for managing JMS Contexts.
 *
 * 2 subclasses are provided with different CDI scope (@RequestScoped and @TransactionScoped).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public abstract class AbstractJMSContext implements Serializable {

    private final transient ReentrantLock lock = new ReentrantLock();
    private final transient Map<String, JMSContext> contexts = new ConcurrentHashMap<>();

    JMSContext getContext(String injectionPointId, JMSInfo info, ConnectionFactory connectionFactory) {
        JMSContext context = contexts.get(injectionPointId);
        if (context == null) {
            lock.lock();
            try {
                context = contexts.get(injectionPointId);
                if (context == null) {
                    context = createContext(info, connectionFactory);
                    contexts.put(injectionPointId, context);
                }
            } finally {
                lock.unlock();
            }
        }
        return context;
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
