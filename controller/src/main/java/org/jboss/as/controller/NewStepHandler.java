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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface NewStepHandler {

    /**
     * Execute this step.  If the operation fails, {@link NewOperationContext#getFailureDescription() context.getFailureDescroption()}
     * must be called, or {@link OperationFailedException} must be thrown, before calling {@link NewOperationContext#completeStep() context.completeStep()}.
     * If the operation succeeded, {@link NewOperationContext#getResult() context.getResult()} should
     * be called and the result populated with the outcome, after which {@link NewOperationContext#completeStep() context.completeStep()}
     * must be called.
     *
     * @param context the operation context
     * @param operation the operation being executed
     * @throws OperationFailedException if the operation failed <b>before</b> calling {@code context.completeStep()}
     */
    void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException;
}
