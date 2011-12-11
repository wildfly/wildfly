/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;


import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.ExtensionDescription;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Base handler for the extension resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddExtensionOperation(ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        return op;
    }

    private final ExtensionContext extensionContext;
    private final boolean parallelBoot;

    /**
     * Create the AbstractAddExtensionHandler
     */
    public ExtensionAddHandler(final ExtensionContext extensionContext, final boolean parallelBoot) {
        if (extensionContext == null) {
            throw MESSAGES.nullVar("extensionContext");
        }
        this.extensionContext = extensionContext;
        this.parallelBoot = parallelBoot;
    }

    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String module = address.getLastElement().getValue();
        resource.getModel().get(ExtensionDescription.MODULE).set(module);

        if (!parallelBoot || !context.isBooting()) {
            initializeExtension(module);
        }
    }

    protected void populateModel(final ModelNode operation, ModelNode model) throws OperationFailedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ExtensionDescription.getExtensionAddOperation(locale);
    }

    void initializeExtension(String module) throws OperationFailedException {
        try {
            for (Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initialize(extensionContext.createTracking(module));
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }
}
