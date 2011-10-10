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
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.HashMap;
import java.util.Map;

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
            throw new OperationFailedException(new ModelNode().set("unknown attribute " + name));
        }
        final ModelNode value = operation.get(ModelDescriptionConstants.VALUE);
        def.getValidator().validateParameter(name, value);
        model.get(name).set(value);
        // Verify the model in a later step
        context.addStep(VERIFY_HANDLER, OperationContext.Stage.VERIFY);
        context.completeStep();
    }

    static class ModelValidationStep implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            for(final AttributeDefinition definition : InterfaceDescription.ROOT_ATTRIBUTES) {
                definition.validateOperation(model);
            }
            context.completeStep();
        }

    }

}
