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

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceNameOperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Interface criteria write-attribute {@code OperationHandler}
 *
 * @author Emanuel Muckenhuber
 */
public final class InterfaceCriteriaWriteHandler implements OperationStepHandler {

    public static final InterfaceCriteriaWriteHandler UPDATE_RUNTIME = new InterfaceCriteriaWriteHandler(true);

    public static final InterfaceCriteriaWriteHandler CONFIG_ONLY = new InterfaceCriteriaWriteHandler(false);

    private static final Map<String, AttributeDefinition> ATTRIBUTES = new HashMap<String, AttributeDefinition>();
    private static final OperationStepHandler VERIFY_HANDLER = new ModelValidationStep();
    private static final ParametersValidator nameValidator = new ParametersValidator();

    static {
        for(final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            ATTRIBUTES.put(def.getName(), def);
        }
    }



    private final boolean updateRuntime;

    private InterfaceCriteriaWriteHandler(final boolean updateRuntime) {
        this.updateRuntime = updateRuntime;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        final ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final AttributeDefinition attributeDefinition = ATTRIBUTES.get(attributeName);
        if (attributeDefinition != null) {
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            attributeDefinition.validateAndSet(syntheticOp, submodel);
        } else {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
        }
        if (updateRuntime) {
            // Require a reload
            context.reloadRequired();
        }
        // Verify the model in a later step
        context.addStep(VERIFY_HANDLER, OperationContext.Stage.MODEL);
        OperationContext.RollbackHandler rollbackHandler = updateRuntime
                ? OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER
                : OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER;
        context.completeStep(rollbackHandler);
    }

    static class ModelValidationStep implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            for(final AttributeDefinition definition : InterfaceDefinition.ROOT_ATTRIBUTES) {
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
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
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
