/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a management security realm's LDAP-based authentication resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LdapAuthenticationResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition CONNECTION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONNECTION, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition BASE_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BASE_DN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition RECURSIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition USER_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USER_DN, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).setDefaultValue(new ModelNode("dn"))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition USERNAME_FILTER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USERNAME_ATTRIBUTE, ModelType.STRING, false)
            .setXmlName("attribute").setAlternatives(ModelDescriptionConstants.ADVANCED_FILTER)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition ADVANCED_FILTER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ADVANCED_FILTER, ModelType.STRING, false)
            .setXmlName("filter")
            .setAlternatives(ModelDescriptionConstants.USERNAME_ATTRIBUTE).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {
        CONNECTION, BASE_DN, RECURSIVE, USER_DN, USERNAME_FILTER, ADVANCED_FILTER
    };

    public LdapAuthenticationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.LDAP),
                ManagementDescription.getResourceDescriptionResolver("core.management.security-realm.authentication.ldap"),
                new SecurityRealmChildAddHandler(true, ATTRIBUTE_DEFINITIONS), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(ATTRIBUTE_DEFINITIONS);
        handler.registerAttributes(resourceRegistration);
    }
}
