/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A handler that simply calls {@link OperationContext#completeStep(OperationContext.RollbackHandler)} with a
 * {@link OperationContext.RollbackHandler#NOOP_ROLLBACK_HANDLER no-op rollback handler}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class NoopOperationStepHandler implements OperationStepHandler {

    /**
     * A {@code NoopOperationStepHandler} that also calls {@link OperationContext#getResult()} thus
     * initializing it to {@link ModelType#UNDEFINED}.
     */
    public static final NoopOperationStepHandler WITH_RESULT = new NoopOperationStepHandler(true);
    /**
     * A {@code NoopOperationStepHandler} that doesn't do anything to establish the operation result node.
     *
     * @see #WITH_RESULT
     */
    public static final NoopOperationStepHandler WITHOUT_RESULT = new NoopOperationStepHandler(false);

    private final boolean setResult;

    private NoopOperationStepHandler(boolean setResult) {
        this.setResult = setResult;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (setResult) {
            context.getResult();
        }
        context.stepCompleted();
    }
}
