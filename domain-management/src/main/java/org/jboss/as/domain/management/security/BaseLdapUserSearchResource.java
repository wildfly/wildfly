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
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A base {@link org.jboss.as.controller.ResourceDefinition} for user to dn search definitions in LDAP.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseLdapUserSearchResource extends SimpleResourceDefinition {

    /*
     * Note: Children will pick which attributes are actually registered, only FORCE is used by all children.
     */

    public static final SimpleAttributeDefinition FORCE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.FORCE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition BASE_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BASE_DN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RECURSIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition USER_DN_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USER_DN_ATTRIBUTE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("dn"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    protected BaseLdapUserSearchResource(final UserSearchType searchType,
            final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler,
            final OperationStepHandler removeHandler) {
        super(PathElement.pathElement(ModelDescriptionConstants.USERNAME_TO_DN, searchType.getModelValue()),
                descriptionResolver, addHandler, removeHandler, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler writeHandler = new SecurityRealmChildWriteAttributeHandler(getAttributeDefinitions());
        writeHandler.registerAttributes(resourceRegistration);
    }

    public abstract AttributeDefinition[] getAttributeDefinitions();

    public enum UserSearchType {

        USERNAME_IS_DN(ModelDescriptionConstants.USERNAME_IS_DN),
        USERNAME_FILTER(ModelDescriptionConstants.USERNAME_FILTER),
        ADVANCED_FILTER(ModelDescriptionConstants.ADVANCED_FILTER);

        private final String modelValue;

        private UserSearchType(final String modelValue) {
            this.modelValue = modelValue;
        }

        public String getModelValue() {
            return modelValue;
        }
    }

}
