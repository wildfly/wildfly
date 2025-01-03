/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.handlers;

import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition.CLASS_NAME;
import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition.CODE;
import static org.wildfly.extension.picketlink.federation.model.handlers.HandlerResourceDefinition.getHandlerType;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.UniqueTypeValidationStepHandler;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class HandlerAddHandler extends ModelOnlyAddStepHandler {

    static final HandlerAddHandler INSTANCE = new HandlerAddHandler();

    private HandlerAddHandler() {
        super(CLASS_NAME, CODE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new UniqueTypeValidationStepHandler(ModelElement.COMMON_HANDLER) {
            @Override
            protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                return getHandlerType(context, model);
            }
        }, OperationContext.Stage.MODEL);
        super.execute(context, operation);
    }
}
