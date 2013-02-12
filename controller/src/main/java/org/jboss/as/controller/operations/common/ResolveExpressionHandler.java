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

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation that resolves an expression (but not against the vault) and returns the resolved value.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ResolveExpressionHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "resolve-expression";

    public static final ResolveExpressionHandler INSTANCE = new ResolveExpressionHandler();

    public static final SimpleAttributeDefinition EXPRESSION = new SimpleAttributeDefinitionBuilder("expression", ModelType.STRING, true)
            .setAllowExpression(true).build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("core"))
        .addParameter(EXPRESSION)
        .setReplyType(ModelType.STRING)
        .allowReturnNull()
        .setReadOnly()
        .setRuntimeOnly()
        .build();


    private ResolveExpressionHandler() {
    }
                                                                   Phase
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Run at Stage.RUNTIME so we get the current values of system properties set by earlier steps in a composite
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode toResolve = EXPRESSION.validateOperation(operation);
                if (toResolve.getType() == ModelType.STRING) {
                    toResolve = ParseUtils.parsePossibleExpression(toResolve.asString());
                }
                try {
                    ModelNode resolved = toResolve.resolve();
                    ModelNode result = context.getResult();
                    if (resolved.isDefined()) {
                        result.set(resolved.asString());
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                } catch (SecurityException e) {
                    throw new OperationFailedException(new ModelNode().set(ControllerMessages.MESSAGES.noPermissionToResolveExpression(toResolve, e)));
                } catch (IllegalStateException e) {
                    throw new OperationFailedException(new ModelNode().set(ControllerMessages.MESSAGES.cannotResolveExpression(toResolve, e)));
                }
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
