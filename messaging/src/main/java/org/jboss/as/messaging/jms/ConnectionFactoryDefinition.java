/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import static java.lang.System.arraycopy;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.getDefinitions;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.AUTO_GROUP;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONNECTION_TTL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.ENTRIES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.GROUP_ID;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.DeprecatedAttributeWriteHandler;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular;

/**
 * JMS Connection Factory resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectionFactoryDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CONNECTION_FACTORY);

    static final AttributeDefinition[] concat(AttributeDefinition[] common, AttributeDefinition... specific) {
        int size = common.length + specific.length;
        AttributeDefinition[] result = new AttributeDefinition[size];
        arraycopy(common, 0, result, 0, common.length);
        arraycopy(specific, 0, result, common.length, specific.length);
        return result;
    }

    public static final AttributeDefinition[] ATTRIBUTES = concat(Regular.ATTRIBUTES, getDefinitions(Common.ATTRIBUTES));

    public static final AttributeDefinition[] NEW_ATTRIBUTES_ADDED_AFTER_1_1_0 = { CALL_FAILOVER_TIMEOUT };

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0 = { ENTRIES, FACTORY_TYPE,
            HA, CALL_TIMEOUT,
            AUTO_GROUP, BLOCK_ON_ACKNOWLEDGE, BLOCK_ON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND, CACHE_LARGE_MESSAGE_CLIENT, CLIENT_FAILURE_CHECK_PERIOD, CLIENT_ID,
            COMPRESS_LARGE_MESSAGES, CONFIRMATION_WINDOW_SIZE, CONNECTION_TTL, CONSUMER_MAX_RATE,
            CONSUMER_WINDOW_SIZE, DUPS_OK_BATCH_SIZE, FAILOVER_ON_INITIAL_CONNECTION, GROUP_ID, MAX_RETRY_INTERVAL, MIN_LARGE_MESSAGE_SIZE, PRE_ACKNOWLEDGE,
            PRODUCER_MAX_RATE, PRODUCER_WINDOW_SIZE, RECONNECT_ATTEMPTS, RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, TRANSACTION_BATCH_SIZE,
            USE_GLOBAL_POOLS};

    static final AttributeDefinition[] READONLY_ATTRIBUTES = { Regular.INITIAL_MESSAGE_PACKET_SIZE };

    private final boolean registerRuntimeOnly;

    public ConnectionFactoryDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTION_FACTORY),
                ConnectionFactoryAdd.INSTANCE,
                ConnectionFactoryRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        for (AttributeDefinition attr : ATTRIBUTES) {
            // deprecated attributes
            if (attr == Common.DISCOVERY_INITIAL_WAIT_TIMEOUT ||
                    attr == Common.FAILOVER_ON_SERVER_SHUTDOWN) {
                registry.registerReadWriteAttribute(attr, null, new DeprecatedAttributeWriteHandler(attr.getName()));
            } else {
                if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                    registry.registerReadWriteAttribute(attr, null, ConnectionFactoryWriteAttributeHandler.INSTANCE);
                }
            }
        }

        if (registerRuntimeOnly) {
            for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
                registry.registerReadOnlyAttribute(attr, ConnectionFactoryReadAttributeHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            SimpleOperationDefinition op = new SimpleOperationDefinition(ConnectionFactoryAddJndiHandler.ADD_JNDI,
                    getResourceDescriptionResolver(),
                    ConnectionFactoryAddJndiHandler.JNDI_BINDING);
            registry.registerOperationHandler(op, ConnectionFactoryAddJndiHandler.INSTANCE);
        }
   }
}