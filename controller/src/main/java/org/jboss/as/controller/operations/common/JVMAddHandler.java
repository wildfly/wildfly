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

import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import org.jboss.as.controller.descriptions.common.JVMDescriptions;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} for the jvm resource add operation.
 *
 * @author Emanuel Muckenhuber
 */
public final class JVMAddHandler extends AbstractAddStepHandler implements DescriptionProvider {
    public static final String OPERATION_NAME = ADD;
    public static final JVMAddHandler INSTANCE = new JVMAddHandler(false);
    public static final JVMAddHandler SERVER_INSTANCE = new JVMAddHandler(true);

    private final boolean server;

    private JVMAddHandler(boolean server) {
        this.server = server;
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        ModelNode jvmType = model.get(JVM_TYPE);
        if (operation.hasDefined(JVM_TYPE)) {
            jvmType.set(operation.get(JVM_TYPE));
        }

        // Handle attributes
        for (final String attr : JVMHandlers.ATTRIBUTES) {
            if (operation.has(attr)) {
                model.get(attr).set(operation.get(attr));
            } else {
                model.get(attr);
            }
        }
        if (server) {
            for (final String attr : JVMHandlers.SERVER_ATTRIBUTES) {
                if (operation.has(attr)) {
                    model.get(attr).set(operation.get(attr));
                } else {
                    model.get(attr);
                }
            }
        }
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(final Locale locale) {
        if (server) {
            return JVMDescriptions.getServerJVMAddDescription(locale);
        }
        return JVMDescriptions.getJVMAddDescription(locale);
    }
}
