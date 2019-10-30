/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
