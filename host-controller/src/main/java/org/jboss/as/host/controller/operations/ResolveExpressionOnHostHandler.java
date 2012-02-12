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

package org.jboss.as.host.controller.operations;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.ResolveExpressionHandler;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation that resolves an expression (but not against the vault) and returns the resolved value.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ResolveExpressionOnHostHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "resolve-expression-on-domain";

    public static final ResolveExpressionOnHostHandler INSTANCE = new ResolveExpressionOnHostHandler();

    private ResolveExpressionOnHostHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Just validate. The real work happens on the servers
        ResolveExpressionHandler.EXPRESSION.validateOperation(operation);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelType valueType = null;
        DescriptionProvider delegate = new DefaultOperationDescriptionProvider(OPERATION_NAME,
                HostRootDescription.getResourceDescriptionResolver("host"), ModelType.STRING, valueType, ResolveExpressionHandler.EXPRESSION);
        ModelNode result = delegate.getModelDescription(locale);
        result.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.NILLABLE).set(true);
        return result;
    }
}
