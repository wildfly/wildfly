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

package org.jboss.as.messaging;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_NONE;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Security role resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class SecurityRoleDefinition extends ModelOnlyResourceDefinition {

    private static SimpleAttributeDefinition create(final String name, final String xmlName) {
        return SimpleAttributeDefinitionBuilder.create(name, BOOLEAN)
                .setXmlName(xmlName)
                .setDefaultValue(new ModelNode(false))
                .setFlags(RESTART_NONE)
                .build();
    }

    static final SimpleAttributeDefinition SEND = create("send", "send");
    static final SimpleAttributeDefinition CONSUME = create("consume", "consume");
    static final SimpleAttributeDefinition CREATE_DURABLE_QUEUE = create("create-durable-queue", "createDurableQueue");
    static final SimpleAttributeDefinition DELETE_DURABLE_QUEUE = create("delete-durable-queue", "deleteDurableQueue");
    static final SimpleAttributeDefinition CREATE_NON_DURABLE_QUEUE = create("create-non-durable-queue", "createNonDurableQueue");
    static final SimpleAttributeDefinition DELETE_NON_DURABLE_QUEUE = create("delete-non-durable-queue", "deleteNonDurableQueue");
    static final SimpleAttributeDefinition MANAGE = SimpleAttributeDefinitionBuilder.create("manage", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setFlags(RESTART_NONE)
            .addAccessConstraint(CommonAttributes.MESSAGING_MANAGEMENT_DEF)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
        SEND,
        CONSUME,
        CREATE_DURABLE_QUEUE,
        DELETE_DURABLE_QUEUE,
        CREATE_NON_DURABLE_QUEUE,
        DELETE_NON_DURABLE_QUEUE,
        MANAGE
    };

    static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create("name", STRING)
            .build();

    static final Map<String, AttributeDefinition> ROLE_ATTRIBUTES_BY_XML_NAME;

    static {
        Map<String, AttributeDefinition> robxn = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition attr : SecurityRoleDefinition.ATTRIBUTES) {
            robxn.put(attr.getXmlName(), attr);
        }
        // Legacy xml names
        robxn.put("createTempQueue", SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE);
        robxn.put("deleteTempQueue", SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE);
        ROLE_ATTRIBUTES_BY_XML_NAME = Collections.unmodifiableMap(robxn);
    }

    static final SecurityRoleDefinition INSTANCE = new SecurityRoleDefinition();

    private SecurityRoleDefinition() {
        super(PathElement.pathElement(CommonAttributes.ROLE),
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.SECURITY_ROLE),
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }
}
