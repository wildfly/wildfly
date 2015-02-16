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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * Core address resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class CoreAddressDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CORE_ADDRESS);

    /**
     * Use the role children instead.
     *
     * @since management model 1.2.0
     */
    @Deprecated
    private static final AttributeDefinition ROLES = ObjectListAttributeDefinition.Builder.of(CommonAttributes.ROLES_ATTR_NAME, SecurityRoleDefinition.getObjectTypeAttributeDefinition())
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .setDeprecated(ModelVersion.create(1,2,0))
            .build();

    private static final AttributeDefinition QUEUE_NAMES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.QUEUE_NAMES, STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition BINDING_NAMES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.BINDING_NAMES, STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition NUMBER_OF_PAGES = create(CommonAttributes.NUMBER_OF_PAGES, INT)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition NUMBER_OF_BYTES_PER_PAGE = create(CommonAttributes.NUMBER_OF_BYTES_PER_PAGE, LONG)
            .setMeasurementUnit(BYTES)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition[] ATTRS = { ROLES, QUEUE_NAMES, BINDING_NAMES, NUMBER_OF_PAGES, NUMBER_OF_BYTES_PER_PAGE };

    // we keep the operation for backwards compatibility but it duplicates the "roles" attributes
    @Deprecated
    public static final String GET_ROLES_AS_JSON = "get-roles-as-json";

    public CoreAddressDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CORE_ADDRESS));
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        for (AttributeDefinition attr : ATTRS) {
            registry.registerReadOnlyAttribute(attr, AddressControlHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        OperationDefinition rolesAsJsonDef = new SimpleOperationDefinitionBuilder(GET_ROLES_AS_JSON, getResourceDescriptionResolver())
                .setReplyType(STRING)
                .withFlags(EnumSet.of(OperationEntry.Flag.READ_ONLY))
                .build();
        registry.registerOperationHandler(rolesAsJsonDef, AddressControlHandler.INSTANCE);
        super.registerOperations(registry);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        ManagementResourceRegistration securityRole = registry.registerSubModel(SecurityRoleDefinition.newReadOnlySecurityRoleDefinition());
        securityRole.setRuntimeOnly(true);
    }
}
