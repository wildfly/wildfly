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

package org.wildfly.extension.messaging.activemq.jms;

import static java.lang.System.arraycopy;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.getDefinitions;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_ALLOWLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLOCKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Regular;

/**
 * Jakarta Messaging Connection Factory resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectionFactoryDefinition extends PersistentResourceDefinition {

    static final String CAPABILITY_NAME = "org.wildfly.messaging.activemq.server.connection-factory";
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(CAPABILITY_NAME, true, ConnectionFactoryService.class)
            .setDynamicNameMapper(DynamicNameMappers.PARENT)
            .build();

    static final AttributeDefinition[] concat(AttributeDefinition[] common, AttributeDefinition... specific) {
        int size = common.length + specific.length;
        AttributeDefinition[] result = new AttributeDefinition[size];
        arraycopy(common, 0, result, 0, common.length);
        arraycopy(specific, 0, result, common.length, specific.length);
        return result;
    }

    public static final AttributeDefinition[] ATTRIBUTES = concat(Regular.ATTRIBUTES, getDefinitions(Common.ATTRIBUTES));

    private final boolean registerRuntimeOnly;

    public ConnectionFactoryDefinition(final boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.CONNECTION_FACTORY_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CONNECTION_FACTORY))
                .setCapabilities(CAPABILITY)
                .setAddHandler(ConnectionFactoryAdd.INSTANCE)
                .setRemoveHandler(ConnectionFactoryRemove.INSTANCE));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        ConnectionFactoryAttributes.registerAliasAttribute(resourceRegistration, false, DESERIALIZATION_WHITELIST, DESERIALIZATION_ALLOWLIST.getName());
        ConnectionFactoryAttributes.registerAliasAttribute(resourceRegistration, false, DESERIALIZATION_BLACKLIST, DESERIALIZATION_BLOCKLIST.getName());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            ConnectionFactoryUpdateJndiHandler.registerOperations(registry, getResourceDescriptionResolver());
        }
   }
}
