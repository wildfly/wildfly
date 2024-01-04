/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;


public class DisableRequiredWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public DisableRequiredWriteAttributeHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) {
        ModelNode submodel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        //do the job
        return (submodel.hasDefined(Constants.ENABLED.getName()) && submodel.get(Constants.ENABLED.getName()).asBoolean()) ||
                org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME.getName().equals(attributeName);
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, Void handback) {
        // no-op
    }
}
