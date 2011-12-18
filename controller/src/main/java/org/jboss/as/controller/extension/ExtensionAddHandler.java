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
package org.jboss.as.controller.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Base handler for the extension resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddExtensionOperation(ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        return op;
    }

    private final ExtensionRegistry extensionRegistry;
    private final boolean parallelBoot;

    /**
     * Create the AbstractAddExtensionHandler
     * @param extensionRegistry registry for extensions
     * @param parallelBoot {@code true} is parallel initialization of extensions is in progress; {@code false} if not
     */
    public ExtensionAddHandler(final ExtensionRegistry extensionRegistry, final boolean parallelBoot) {
        assert extensionRegistry != null : "extensionRegistry is null";
        this.extensionRegistry = extensionRegistry;
        this.parallelBoot = parallelBoot;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String moduleName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        ExtensionResource resource = new ExtensionResource(moduleName, extensionRegistry);
        context.addResource(PathAddress.EMPTY_ADDRESS, resource);

        if (!parallelBoot || !context.isBooting()) {
            initializeExtension(moduleName);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    void initializeExtension(String module) throws OperationFailedException {
        try {
            for (Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    if (!extensionRegistry.getExtensionModuleNames().contains(module)) {
                        // This extension wasn't handled by the standalone.xml or domain.xml parsing logic, so we
                        // need to initialize its parsers so we can display what XML namespaces it supports
                        extension.initializeParsers(extensionRegistry.getExtensionParsingContext(module, null));
                    }
                    extension.initialize(extensionRegistry.getExtensionContext(module));
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
            }
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }
    }
}
