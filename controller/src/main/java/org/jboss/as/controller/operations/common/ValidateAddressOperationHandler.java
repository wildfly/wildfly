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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ValidateAddressOperationHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "check-address";
    public static final ValidateAddressOperationHandler INSTANCE = new ValidateAddressOperationHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode address = operation.require(VALUE);
        final PathAddress pathAddr = PathAddress.pathAddress(address);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        try {
            resource.navigate(pathAddr);
            context.getResult().get(VALID).set(true);
        } catch(NoSuchElementException e) {
            context.getResult().get(VALID).set(false);
            context.getResult().get(PROBLEM).set(e.getMessage());
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getCheckAddressOperation(locale);
    }
}
