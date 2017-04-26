/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for {@code read-attribute} that delegates to a different attribute.
 *
 * TODO move this into WildFly Core for reuse.
 *
 * @author Brian Stansberry
 */
class SimpleAliasReadAttributeHandler implements OperationStepHandler {

    private static final SimpleAttributeDefinition INCLUDE_DEFAULTS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_DEFAULTS, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode(true))
            .build();

    private final SimpleAttributeDefinition aliasedAttribute;

    SimpleAliasReadAttributeHandler(SimpleAttributeDefinition aliasedAttribute) {
        this.aliasedAttribute = aliasedAttribute;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final boolean defaults = INCLUDE_DEFAULTS.resolveModelAttribute(context,operation).asBoolean();
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
        final ModelNode subModel = resource.getModel();
        if (subModel.hasDefined(aliasedAttribute.getName())) {
            final ModelNode result = subModel.get(aliasedAttribute.getName());
            context.getResult().set(result);
        } else if (defaults && aliasedAttribute.getDefaultValue() != null) {
            // No defined value in the model. See if we should reply with a default from the metadata,
            // reply with undefined, or fail because it's a non-existent attribute name
            context.getResult().set(aliasedAttribute.getDefaultValue());
        } else {
            // model had no defined value, but we treat its existence in the model or the metadata
            // as proof that it's a legit attribute name
            context.getResult(); // this initializes the "result" to ModelType.UNDEFINED
        }
    }
}
