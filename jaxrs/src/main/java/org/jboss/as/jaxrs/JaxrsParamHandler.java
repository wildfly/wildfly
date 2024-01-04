/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * An AbstractWriteAttributeHandler extension for updating basic RESTEasy config attributes
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
final class JaxrsParamHandler extends AbstractWriteAttributeHandler<Void> {

    public JaxrsParamHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder)
                    throws OperationFailedException {

        // Currently all attributes are set as context parameters and therefore require a reload or at least a re-deploy
        return !isSameValue(context, resolvedValue, currentValue, attributeName);
    }

    private boolean isSameValue(OperationContext context, ModelNode resolvedValue, ModelNode currentValue, String attributeName)
            throws OperationFailedException {
        if (resolvedValue.equals(getAttributeDefinition(attributeName).resolveValue(context, currentValue))) {
            return true;
        }
        if (!currentValue.isDefined()) {
            return resolvedValue.equals(getAttributeDefinition(attributeName).getDefaultValue());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        // Nothing to do, as nothing was changed
    }
}
