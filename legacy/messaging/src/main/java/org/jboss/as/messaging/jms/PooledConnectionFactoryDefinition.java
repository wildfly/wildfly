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
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.JNDI_PARAMS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.SETUP_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.SETUP_INTERVAL;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.TRANSACTION;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_JNDI;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_LOCAL_TX;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.messaging.AlternativeAttributeCheckHandler;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.dmr.ModelNode;

/**
 * JMS pooled Connection Factory resource definition.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PooledConnectionFactoryDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);

    // the generation of the Pooled CF attributes is a bit ugly but it is with purpose:
    // * factorize the attributes which are common between the regular CF and the pooled CF
    // * keep in a single place the subtle differences (e.g. different default values for reconnect-attempts between
    //   the regular and pooled CF
    // * define the attributes in the *same order than the XSD* to write them to the XML configuration by simply iterating over the array
    private static ConnectionFactoryAttribute[] define(ConnectionFactoryAttribute[] specific, ConnectionFactoryAttribute... common) {
        int size = common.length + specific.length;
        ConnectionFactoryAttribute[] result = new ConnectionFactoryAttribute[size];
        arraycopy(specific, 0, result, 0, specific.length);
        for (int i = 0; i < common.length; i++) {
            ConnectionFactoryAttribute attr = common[i];
            AttributeDefinition definition = attr.getDefinition();

            ConnectionFactoryAttribute newAttr;
            // replace the reconnect-attempts attribute to use a different default value for pooled CF
            if (definition == Common.RECONNECT_ATTEMPTS) {
                AttributeDefinition copy = copy(Pooled.RECONNECT_ATTEMPTS, AttributeAccess.Flag.RESTART_ALL_SERVICES);
                newAttr = ConnectionFactoryAttribute.create(copy, Pooled.RECONNECT_ATTEMPTS_PROP_NAME, true);
            } else {
                AttributeDefinition copy = copy(definition, AttributeAccess.Flag.RESTART_ALL_SERVICES);
                newAttr = ConnectionFactoryAttribute.create(copy, attr.getPropertyName(), attr.isResourceAdapterProperty(), attr.isInboundConfig());
            }
            result[specific.length + i] = newAttr;
        }
        return result;
    }

    private static AttributeDefinition copy(AttributeDefinition attribute, AttributeAccess.Flag flag) {
        AbstractAttributeDefinitionBuilder builder;
        if (attribute instanceof  SimpleListAttributeDefinition) {
            builder =  new SimpleListAttributeDefinition.Builder((SimpleListAttributeDefinition)attribute);
            // TODO remove once WFCORE-95 is fixed
            ((SimpleListAttributeDefinition.Builder)builder).setListValidator(attribute.getValidator());
        } else if (attribute instanceof  SimpleMapAttributeDefinition) {
            builder = new SimpleMapAttributeDefinition.Builder((SimpleMapAttributeDefinition)attribute);
        } else if (attribute instanceof PrimitiveListAttributeDefinition) {
            builder = new PrimitiveListAttributeDefinition.Builder((PrimitiveListAttributeDefinition)attribute);
            // TODO remove once WFCORE-95 is fixed
            ((PrimitiveListAttributeDefinition.Builder)builder).setListValidator(attribute.getValidator());
        } else {
            builder = new SimpleAttributeDefinitionBuilder((SimpleAttributeDefinition)attribute);
        }
        builder.setFlags(flag);
        return builder.build();
    }

    public static final ConnectionFactoryAttribute[] ATTRIBUTES = define(Pooled.ATTRIBUTES, Common.ATTRIBUTES);

    // attributes added to the pooled cf resources *after* 1.1.0
    public static final AttributeDefinition[] ATTRIBUTES_ADDED_IN_1_2_0 = new AttributeDefinition[]{INITIAL_CONNECT_ATTEMPTS,
            INITIAL_MESSAGE_PACKET_SIZE,
            COMPRESS_LARGE_MESSAGES,
            USE_AUTO_RECOVERY};

    // attributes with expression supported added *after* 1.1.0
    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = { ENTRIES, CALL_TIMEOUT,
            AUTO_GROUP, BLOCK_ON_ACKNOWLEDGE, BLOCK_ON_DURABLE_SEND, BLOCK_ON_NON_DURABLE_SEND, CACHE_LARGE_MESSAGE_CLIENT, CLIENT_FAILURE_CHECK_PERIOD, CLIENT_ID,
            CONFIRMATION_WINDOW_SIZE, CONNECTION_TTL, CONSUMER_MAX_RATE,
            CONSUMER_WINDOW_SIZE, DUPS_OK_BATCH_SIZE, FAILOVER_ON_INITIAL_CONNECTION, GROUP_ID, HA, MAX_RETRY_INTERVAL, MIN_LARGE_MESSAGE_SIZE, PRE_ACKNOWLEDGE,
            PRODUCER_MAX_RATE, PRODUCER_WINDOW_SIZE, RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, TRANSACTION_BATCH_SIZE,
            USE_GLOBAL_POOLS, // end of common attributes
            JNDI_PARAMS, RECONNECT_ATTEMPTS, SETUP_ATTEMPTS, SETUP_INTERVAL,
            TRANSACTION, USE_JNDI, USE_LOCAL_TX};

    public static final PooledConnectionFactoryDefinition INSTANCE = new PooledConnectionFactoryDefinition();

    private PooledConnectionFactoryDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.POOLED_CONNECTION_FACTORY),
                new ModelOnlyAddStepHandler(getDefinitions(PooledConnectionFactoryDefinition.ATTRIBUTES)) {
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        super.populateModel(operation, model);
                        AlternativeAttributeCheckHandler.checkAlternatives(operation, Common.CONNECTOR.getName(), Common.DISCOVERY_GROUP_NAME.getName(), false);
                    }
                },
                getDefinitions(PooledConnectionFactoryDefinition.ATTRIBUTES));
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}