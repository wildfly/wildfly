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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Abstract resource definition for database authorization and authentication resource.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public abstract class DatabaseResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition REF = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REF, ModelType.STRING, false)
    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition PLAIN_TEXT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PLAIN_TEXT, ModelType.BOOLEAN, false)
            .setDefaultValue(new ModelNode(false)).
            setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition USERNAME_FIELD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.USERNAME_FIELD, ModelType.STRING, false)
            .setXmlName("username-field")
            .setAlternatives(ModelDescriptionConstants.USERNAME_FIELD)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setValidateNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();


    public static final SimpleAttributeDefinition TABLE_FIELD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SIMPLE_SELECT_TABLE, ModelType.STRING, false)
            .setXmlName("name")
            .setAlternatives(ModelDescriptionConstants.SIMPLE_SELECT_TABLE)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setValidateNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public DatabaseResourceDefinition(final PathElement pathElement,
            final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler,
            final OperationStepHandler removeHandler, final OperationEntry.Flag addRestartLevel,
            final OperationEntry.Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
    }


    protected class DatabaseResourceWriteHandler extends SecurityRealmChildWriteAttributeHandler {

        DatabaseResourceWriteHandler() {
            super(getAttributeDefinitions());
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode ignored) throws OperationFailedException {
                    final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                    final ModelNode model = resource.getModel();
                    validateAttributeCombination(model);

                    context.completeStep();
                }
            }, OperationContext.Stage.MODEL);
            super.execute(context, operation);
        }
    }

    abstract AttributeDefinition[] getAttributeDefinitions();

    abstract void validateAttributeCombination(ModelNode operation) throws OperationFailedException;
}
