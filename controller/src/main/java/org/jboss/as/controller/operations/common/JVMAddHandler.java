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
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.JVMDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} for the jvm resource add operation.
 *
 * @author Emanuel Muckenhuber
 */
public final class JVMAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;
    public static final JVMAddHandler INSTANCE = new JVMAddHandler(false);
    public static final JVMAddHandler SERVER_INSTANCE = new JVMAddHandler(true);

    private final boolean server;

    private JVMAddHandler(boolean server) {
        this.server = server;
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        ModelNode subModel = context.getSubModel();
        ModelNode jvmType = subModel.get(JVM_TYPE);
        if(operation.hasDefined(JVM_TYPE)) {
            jvmType.set(operation.get(JVM_TYPE));
        }

        // Handle attributes
        for (final String attr : JVMHandlers.ATTRIBUTES) {
            if(operation.has(attr)) {
                subModel.get(attr).set(operation.get(attr));
            } else {
                subModel.get(attr);
            }
        }
        if(server) {
            for(final String attr : JVMHandlers.SERVER_ATTRIBUTES) {
                if(operation.has(attr)) {
                    subModel.get(attr).set(operation.get(attr));
                } else {
                    subModel.get(attr);
                }
            }
        }

        resultHandler.handleResultComplete();

        return new BasicOperationResult(compensatingOperation);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode getModelDescription(final Locale locale) {
        if(server) {
            return JVMDescriptions.getServerJVMAddDescription(locale);
        }
        return JVMDescriptions.getJVMAddDescription(locale);
    }

}
