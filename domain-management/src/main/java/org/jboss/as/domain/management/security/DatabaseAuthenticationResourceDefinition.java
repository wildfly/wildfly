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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a security realm's database authentication resource.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseAuthenticationResourceDefinition extends DatabaseResourceDefinition {

    public static final SimpleAttributeDefinition SIMPLE_SELECT_USERS_FIELD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SIMPLE_SELECT_USERS, ModelType.BOOLEAN, false)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition PASSWORD_FIELD = new SimpleAttributeDefinitionBuilder(
            ModelDescriptionConstants.PASSWORD_FIELD, ModelType.STRING, false).setXmlName("password-field")
            .setAlternatives(ModelDescriptionConstants.PASSWORD_FIELD)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false)).setValidateNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition SQL_SELECT_USERS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SQL_SELECT_USERS, ModelType.STRING, false)
            .setXmlName("sql")
            .setAlternatives(ModelDescriptionConstants.SQL_SELECT_USERS_ROLES_STATEMENT)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setValidateNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {
        CONNECTION, PLAIN_TEXT, SIMPLE_SELECT_USERS_FIELD, SQL_SELECT_USERS, USERNAME_FIELD, PASSWORD_FIELD,TABLE_FIELD
    };

    public DatabaseAuthenticationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.DATABASE),
                ManagementDescription.getResourceDescriptionResolver("core.management.security-realm.authentication.database"),
                new DatabaseResourceAddHandler(true,ATTRIBUTE_DEFINITIONS), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    AttributeDefinition[] getAttributeDefinitions() {
        return ATTRIBUTE_DEFINITIONS;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new DatabaseResourceWriteHandler();
        handler.registerAttributes(resourceRegistration);
    }

    static class DatabaseResourceAddHandler extends SecurityRealmChildAddHandler {


        DatabaseResourceAddHandler(boolean validate, AttributeDefinition[] attributeDefinitions) {
            super(validate, attributeDefinitions);
        }

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            validateAutenticationAttributeCombination(operation);
            super.updateModel(context, operation);
        }
    }


    void validateAttributeCombination(ModelNode operation) throws OperationFailedException {
        validateAttributeCombination(operation);
    }

    static void validateAutenticationAttributeCombination(ModelNode operation) throws OperationFailedException {
        boolean simpleSelectDefined = operation.hasDefined(ModelDescriptionConstants.USERNAME_FIELD)
                && operation.hasDefined(ModelDescriptionConstants.PASSWORD_FIELD)
                && operation.hasDefined(ModelDescriptionConstants.SIMPLE_SELECT_TABLE);
        boolean sqlStatementDefined = operation.hasDefined(ModelDescriptionConstants.SQL_SELECT_USERS);
        if (simpleSelectDefined && sqlStatementDefined) {
            throw MESSAGES.operationFailedOnlyOneOfRequired(ModelDescriptionConstants.SIMPLE_SELECT_USERS,
                    ModelDescriptionConstants.SQL_SELECT_USERS);
        } else if ((!simpleSelectDefined && !sqlStatementDefined)) {
            throw MESSAGES.operationFailedOneOfRequired(ModelDescriptionConstants.SIMPLE_SELECT_USERS,
                    ModelDescriptionConstants.SQL_SELECT_USERS_ROLES_STATEMENT);
        }
    }
}
