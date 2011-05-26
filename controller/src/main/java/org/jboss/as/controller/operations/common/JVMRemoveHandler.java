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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.JVMDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} for the jvm resource remove operation.
 *
 * @author Emanuel Muckenhuber
 */
public final class JVMRemoveHandler implements NewStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;
    public static final JVMRemoveHandler INSTANCE = new JVMRemoveHandler();



    @Override
    public void execute(NewOperationContext context, ModelNode operation) {
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        context.removeModel(PathAddress.EMPTY_ADDRESS);

        context.completeStep();
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return JVMDescriptions.getJVMRemoveDescription(locale);
    }
}
