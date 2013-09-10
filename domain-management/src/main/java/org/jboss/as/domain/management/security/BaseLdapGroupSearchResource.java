/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A base {@link org.jboss.as.controller.ResourceDefinition} for group search definitions in LDAP.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseLdapGroupSearchResource extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition GROUP_NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.GROUP_NAME, ModelType.STRING, true)
            .setValidator(new EnumValidator<GroupName>(GroupName.class, true, true, GroupName.DISTINGUISHED_NAME, GroupName.SIMPLE))
            .setDefaultValue(new ModelNode(GroupName.SIMPLE.toString()))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ITERATIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ITERATIVE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition GROUP_DN_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.GROUP_DN_ATTRIBUTE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("dn"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition GROUP_NAME_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.GROUP_NAME_ATTRIBUTE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("uid"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    protected BaseLdapGroupSearchResource(final GroupSearchType searchType,
            final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler,
            final OperationStepHandler removeHandler) {
        super(PathElement.pathElement(ModelDescriptionConstants.GROUP_SEARCH, searchType.getModelValue()),
                descriptionResolver, addHandler, removeHandler, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler writeHandler = new SecurityRealmChildWriteAttributeHandler(getAttributeDefinitions());
        writeHandler.registerAttributes(resourceRegistration);
    }

    public abstract AttributeDefinition[] getAttributeDefinitions();

    public enum GroupName {
        DISTINGUISHED_NAME, SIMPLE
    }

    public enum GroupSearchType {

        GROUP_TO_PRINCIPAL(ModelDescriptionConstants.GROUP_TO_PRINCIPAL),
        PRINCIPAL_TO_GROUP(ModelDescriptionConstants.PRINCIPAL_TO_GROUP);

        private final String modelValue;

        private GroupSearchType(final String modelValue) {
            this.modelValue = modelValue;
        }

        public String getModelValue() {
            return modelValue;
        }
    }

}
