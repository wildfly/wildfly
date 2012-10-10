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
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.DeprecatedAttributeWriteHandler;
import org.jboss.as.messaging.MessagingDescriptions;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.dmr.ModelNode;

/**
 * JMS pooled Connection Factory resource definition.
 *
 * TODO once it will be possible to set flags on attribute when they are registered,
 * this resource needs to be simplified, removings its description provider (idem for its add &amp;
 * remove operations).
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PooledConnectionFactoryDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);

    // the generation of the Pooled CF attributes is a bit ugly but it is with purpose:
    // * factorize the attributes which are common between the regular CF and the pooled CF
    // * keep in a single place the subtle differences (e.g. different default values for reconnect-attempts between
    //   the regular and pooled CF
    // * define the attributes in the *same order than the XSD* to write them to the XML configuration by simply iterating over the array
    private static ConnectionFactoryAttribute[] define(ConnectionFactoryAttribute[] common, ConnectionFactoryAttribute... specific) {
        int size = common.length + specific.length;
        ConnectionFactoryAttribute[] result = new ConnectionFactoryAttribute[size];
        arraycopy(common, 0, result, 0, common.length);
        arraycopy(specific, 0, result, common.length, specific.length);
        // replace the reconnect-attempts attribute to use a different default value for pooled CF
        for (int i = 0; i < result.length; i++) {
            ConnectionFactoryAttribute attribute = result[i];
            if (attribute.getDefinition() == CommonAttributes.RECONNECT_ATTEMPTS) {
                result[i] = ConnectionFactoryAttribute.create(Pooled.RECONNECT_ATTEMPTS, Pooled.RECONNECT_ATTEMPTS_PROP_NAME, true);
            }
        }
        return result;
    }

    public static final ConnectionFactoryAttribute[] ATTRIBUTES = define(Pooled.ATTRIBUTES, Common.ATTRIBUTES);


    private static final DescriptionProvider DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return MessagingDescriptions.getPooledConnectionFactory(locale);
        }
    };

    private final boolean registerRuntimeOnly;

    public PooledConnectionFactoryDefinition(final boolean registerRuntimeOnly) {
        super(PATH, DESC);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        //FIXME how to set these flags to the pooled CF attributes?
        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        for (AttributeDefinition attr : getDefinitions(ATTRIBUTES)) {
            // deprecated attribute
            if (attr == Common.DISCOVERY_INITIAL_WAIT_TIMEOUT ||
                    attr == Common.FAILOVER_ON_SERVER_SHUTDOWN) {
                registry.registerReadWriteAttribute(attr, null, new DeprecatedAttributeWriteHandler(attr.getName()));
            } else {
                if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                    registry.registerReadWriteAttribute(attr.getName(), null, PooledConnectionFactoryWriteAttributeHandler.INSTANCE, flags);
                }
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        super.registerAddOperation(registry, PooledConnectionFactoryAdd.INSTANCE, OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(registry, PooledConnectionFactoryRemove.INSTANCE,  OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }
}