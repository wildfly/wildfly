/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.vault.module;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestVaultResolveExpressionHandler implements OperationStepHandler {

    public static final OperationDefinition RESOLVE = new SimpleOperationDefinitionBuilder("test", new NonResolvingResourceDescriptionResolver())
        .addParameter(TestVaultResolveExpressionHandler.PARAM_EXPRESSION)
        .build();

    public static final AttributeDefinition PARAM_EXPRESSION = SimpleAttributeDefinitionBuilder.create("expression", ModelType.STRING, false)
        .setAllowExpression(true)
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        TestVaultResolveExpressionHandler.PARAM_EXPRESSION.validateOperation(operation);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation)
                    throws OperationFailedException {
                context.getResult().set(context.resolveExpressions(operation.get(TestVaultResolveExpressionHandler.PARAM_EXPRESSION.getName())));
                context.stepCompleted();
            }
        }, Stage.RUNTIME);
        context.stepCompleted();
    }

}
