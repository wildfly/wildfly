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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;

/**
 * Attribute handler for cache-container resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheContainerWriteAttributeHandler implements OperationStepHandler {

    public static final CacheContainerWriteAttributeHandler INSTANCE = new CacheContainerWriteAttributeHandler();
    private final ParametersValidator nameValidator = new ParametersValidator();

    private final Map<String, AttributeDefinition> attributeDefinitions;

    public CacheContainerWriteAttributeHandler() {
        this(CacheContainerResourceDefinition.ATTRIBUTES);
    }

    public CacheContainerWriteAttributeHandler(final AttributeDefinition... definitions) {
        this.attributeDefinitions = new HashMap<>();
        for (AttributeDefinition def : definitions) {
            this.attributeDefinitions.put(def.getName(), def);
        }
    }

    /**
     * An attribute write handler which performs special processing for ALIAS attributes.
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws OperationFailedException
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        this.nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value
        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();

        final AttributeDefinition attributeDefinition = this.attributeDefinitions.get(attributeName);
        if (attributeDefinition != null) {
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            attributeDefinition.validateAndSet(syntheticOp, submodel);
        } else {
            submodel.get(attributeName).set(newValue);
        }

        // since we modified the model, set reload required
        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) {
                    context.reloadRequired();
                    context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }


     /**
      * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added. This default implementation
      * returns {@code true} if the {@link org.jboss.as.controller.OperationContext#getProcessType()}  process type} is
      * a server and {@link OperationContext#isBooting() context.isBooting()} returns {@code false}.
      *
      * @param context operation context
      * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
      */
     protected boolean requiresRuntime(OperationContext context) {
         return context.getProcessType().isServer() && !context.isBooting();
     }
}
