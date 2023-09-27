/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import java.util.HashSet;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class JMSServices {

    private static final String[] NO_BINDINGS = new String[0];

    private static final String JMS = "jms";
    private static final String JMS_MANAGER = "manager";
    private static final String JMS_QUEUE_BASE = "queue";
    private static final String JMS_TOPIC_BASE = "topic";
    private static final String JMS_CF_BASE = "connection-factory";
    public static final String JMS_POOLED_CF_BASE = "pooled-connection-factory";

    public static ServiceName getJmsManagerBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(JMS).append(JMS_MANAGER);
    }

    public static ServiceName getJmsQueueBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(JMS).append(JMS_QUEUE_BASE);
    }

    public static ServiceName getJmsTopicBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(JMS).append(JMS_TOPIC_BASE);
    }

    public static ServiceName getConnectionFactoryBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(JMS).append(JMS_CF_BASE);
    }

    public static ServiceName getPooledConnectionFactoryBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(JMS).append(JMS_POOLED_CF_BASE);
    }

    public static String[] getJndiBindings(final ModelNode node) {
        if (node.isDefined()) {
            final Set<String> bindings = new HashSet<String>();
            for (final ModelNode entry : node.asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }
}
