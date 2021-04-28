/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.deployment.injection;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;

/**
 * Abstract class for managing JMS Contexts.
 *
 * 2 subclasses are provided with different CDI scope (@RequestScoped and @TransactionScoped).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public abstract class AbstractJMSContext implements Serializable {

    private final Map<String, JMSContext> contexts = new HashMap<>();

    synchronized JMSContext getContext(String injectionPointId, JMSInfo info, ConnectionFactory connectionFactory) {
        JMSContext context = contexts.get(injectionPointId);
        if (context == null) {
            context = createContext(info, connectionFactory);
            contexts.put(injectionPointId, context);
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
