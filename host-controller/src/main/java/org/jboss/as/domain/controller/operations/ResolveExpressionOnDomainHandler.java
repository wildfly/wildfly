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

package org.jboss.as.domain.controller.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.operations.common.ResolveExpressionHandler;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.descriptions.DomainResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation that resolves an expression (but not against the vault) and returns the resolved value.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ResolveExpressionOnDomainHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "resolve-expression-on-domain";

    public static final ResolveExpressionOnDomainHandler INSTANCE = new ResolveExpressionOnDomainHandler();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, DomainResolver.getResolver("domain"))
            .addParameter(ResolveExpressionHandler.EXPRESSION)
            .setReplyType(ModelType.STRING)
            .allowReturnNull()
            .setReadOnly()
            .withFlag(OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS)
            .build();


    private ResolveExpressionOnDomainHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Just validate. The real work happens on the servers
        ResolveExpressionHandler.EXPRESSION.validateOperation(operation);

        context.stepCompleted();
    }
}
