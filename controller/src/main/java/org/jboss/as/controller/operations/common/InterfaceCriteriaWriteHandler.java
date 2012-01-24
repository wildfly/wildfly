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

package org.jboss.as.controller.operations.common;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import org.jboss.dmr.ModelType;

/**
 * Interface criteria write-attribute {@code OperationHandler}
 *
 * @author Emanuel Muckenhuber
 */
public final class InterfaceCriteriaWriteHandler implements OperationStepHandler {

    public static final OperationStepHandler INSTANCE = new InterfaceCriteriaWriteHandler();

    private static final Map<String, AttributeDefinition> ATTRIBUTES = new HashMap<String, AttributeDefinition>();
    private static final OperationStepHandler VERIFY_HANDLER = new ModelValidationStep();

    static {
        for(final AttributeDefinition def : InterfaceDescription.ROOT_ATTRIBUTES) {
            ATTRIBUTES.put(def.getName(), def);
        }
    }

    public static void register(final ManagementResourceRegistration registration) {
        for(final AttributeDefinition def : InterfaceDescription.ROOT_ATTRIBUTES) {
            registration.registerReadWriteAttribute(def, null, INSTANCE);
        }
    }

    private InterfaceCriteriaWriteHandler() {
        //
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();
        final AttributeDefinition def = ATTRIBUTES.get(name);
        if(def == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(name)));
        }
        final ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
        def.getValidator().validateParameter(name, value);
        model.get(name).set(value);
        context.reloadRequired();
        // Verify the model in a later step
        context.addStep(VERIFY_HANDLER, OperationContext.Stage.MODEL);
        if (context.completeStep() != OperationContext.ResultAction.KEEP) {
            context.revertReloadRequired();
        }
    }

    static class ModelValidationStep implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            for(final AttributeDefinition definition : InterfaceDescription.ROOT_ATTRIBUTES) {
                final String attributeName = definition.getName();
                final boolean has = model.hasDefined(attributeName);
                if(! has && isRequired(definition, model)) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.required(attributeName)));
                }
                if(has) {
                    // Just ignore 'false'
                    if(definition.getType() == ModelType.BOOLEAN && ! model.get(attributeName).asBoolean()) {
                        continue;
                    }
                    if(! isAllowed(definition, model)) {
                        // TODO probably move this into AttributeDefinition
                        String[] alts = definition.getAlternatives();
                        StringBuilder sb = null;
                        if (alts != null) {
                            for (String alt : alts) {
                                if (model.hasDefined(alt)) {
                                    if (sb == null) {
                                        sb = new StringBuilder();
                                    } else {
                                        sb.append(", ");
                                    }
                                    sb.append(alt);
                                }
                            }
                        }
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidAttributeCombo(attributeName, sb)));
                    }
                }
            }
            context.completeStep();
        }

        boolean isRequired(final AttributeDefinition def, final ModelNode model) {
            final boolean required = ! def.isAllowNull();
            return required ? ! hasAlternative(def.getAlternatives(), model, true) : required;
        }

        boolean isAllowed(final AttributeDefinition def, final ModelNode model) {
            final String[] alternatives = def.getAlternatives();
            if(alternatives != null) {
                for(final String alternative : alternatives) {
                    if(model.hasDefined(alternative)) {
                        if(ATTRIBUTES.get(alternative).getType() == ModelType.BOOLEAN) {
                            return ! model.get(alternative).asBoolean();
                        }
                        return false;
                    }
                }
            }
            return true;
        }

        boolean hasAlternative(final String[] alternatives,  ModelNode operationObject, boolean ignoreBoolean) {
            if(alternatives != null) {
                for(final String alternative : alternatives) {
                    if(operationObject.hasDefined(alternative)) {
                        if(ignoreBoolean) {
                            if(operationObject.get(alternative).getType() == ModelType.BOOLEAN) {
                                return operationObject.get(alternative).asBoolean();
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
