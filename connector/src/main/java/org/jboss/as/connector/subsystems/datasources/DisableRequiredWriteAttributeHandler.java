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

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;


public class DisableRequiredWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public DisableRequiredWriteAttributeHandler() {
        super();
    }

    public DisableRequiredWriteAttributeHandler(final ParameterValidator validator) {
        super(validator);
    }

    public DisableRequiredWriteAttributeHandler(final AttributeDefinition... definitions) {
        super(definitions);
    }

    public DisableRequiredWriteAttributeHandler(final ParameterValidator unresolvedValidator, final ParameterValidator resolvedValidator) {
        super(unresolvedValidator, resolvedValidator);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandback) {
        ModelNode submodel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        if ((submodel.hasDefined(Constants.ENABLED.getName()) && submodel.get(Constants.ENABLED.getName()).asBoolean()) ||
                Constants.JNDINAME.getName().equals(attributeName)) {
           return true;
        } else {
            //do the job
            return false;
        }
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, Void handback) {
        // no-op
    }
}
