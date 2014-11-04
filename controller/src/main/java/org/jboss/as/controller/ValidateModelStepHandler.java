/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.controller;

import java.util.Set;

import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
class ValidateModelStepHandler implements OperationStepHandler {
    public static final ValidateModelStepHandler INSTANCE = new ValidateModelStepHandler();

    private ValidateModelStepHandler() {

    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
        final ModelNode model = resource.getModel();
        final ImmutableManagementResourceRegistration resourceRegistration = context.getResourceRegistration();
        final Set<String> attributeNames = resourceRegistration.getAttributeNames(PathAddress.EMPTY_ADDRESS);
        for (String attributeName : attributeNames) {
            final boolean has = model.hasDefined(attributeName);
            AttributeAccess access = context.getResourceRegistration().getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);
            if (access.getStorageType() != AttributeAccess.Storage.CONFIGURATION){
                continue;
            }
            AttributeDefinition attr = access.getAttributeDefinition();
            if (!has && isRequired(attr, model)) {
                throw new OperationFailedException(ControllerMessages.MESSAGES.required(attributeName));
            }
            if (!has) {
                continue;
            }

            if (attr.getRequires() != null) {
                for (String required : attr.getRequires()) {
                    if (!model.hasDefined(required)) {
                        throw ControllerMessages.MESSAGES.requiredAttributeNotSet(required, attr.getName());
                    }
                }
            }

            if (!isAllowed(attr, model)) {
                String[] alts = attr.getAlternatives();
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
                throw new OperationFailedException(ControllerMessages.MESSAGES.invalidAttributeCombo(attributeName, sb));
            }
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    boolean isRequired(final AttributeDefinition def, final ModelNode model) {
        final boolean required = !def.isAllowNull() && !def.isResourceOnly();
        return required ? !hasAlternative(def.getAlternatives(), model) : required;
    }

    boolean isAllowed(final AttributeDefinition def, final ModelNode model) {
        final String[] alternatives = def.getAlternatives();
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                if (model.hasDefined(alternative)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean hasAlternative(final String[] alternatives, ModelNode operationObject) {
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                if (operationObject.hasDefined(alternative)) {
                    return true;
                }
            }
        }
        return false;
    }

}

