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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Base handler for the extension resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionAddHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = ADD;

    private final ExtensionRegistry extensionRegistry;
    private final boolean parallelBoot;
    private final boolean standalone;
    private final boolean slaveHC;

    /**
     * Create the AbstractAddExtensionHandler
     * @param extensionRegistry registry for extensions
     * @param parallelBoot {@code true} is parallel initialization of extensions is in progress; {@code false} if not
     * @param slaveHC {@code true} if this handler will execute in a slave HostController
     */
    public ExtensionAddHandler(final ExtensionRegistry extensionRegistry, final boolean parallelBoot, boolean standalone, boolean slaveHC) {
        assert extensionRegistry != null : "extensionRegistry is null";
        this.extensionRegistry = extensionRegistry;
        this.parallelBoot = parallelBoot;
        this.slaveHC = slaveHC;
        this.standalone = standalone;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String moduleName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        ExtensionResource resource = new ExtensionResource(moduleName, extensionRegistry);

        context.addResource(PathAddress.EMPTY_ADDRESS, resource);

        if (!parallelBoot || !context.isBooting()) {
            initializeExtension(moduleName);
            if (slaveHC && !context.isBooting()) {
                ModelNode subsystems = new ModelNode();
                extensionRegistry.recordSubsystemVersions(moduleName, subsystems);
                context.getResult().set(subsystems);
            }
        }

        context.stepCompleted();
    }

    void initializeExtension(String module) throws OperationFailedException {
        try {
            boolean unknownModule = false;
            for (Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                ClassLoader oldTccl = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(extension.getClass());
                try {
                    if (unknownModule || !extensionRegistry.getExtensionModuleNames().contains(module)) {
                        // This extension wasn't handled by the standalone.xml or domain.xml parsing logic, so we
                        // need to initialize its parsers so we can display what XML namespaces it supports
                        extension.initializeParsers(extensionRegistry.getExtensionParsingContext(module, null));
                        // AS7-6190 - ensure we initialize parsers for other extensions from this module
                        // now that we know the registry was unaware of the module
                        unknownModule = true;
                    }
                    extension.initialize(extensionRegistry.getExtensionContext(module, !standalone && !slaveHC));
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
                }
            }
        } catch (ModuleNotFoundException e) {
            // Treat this as a user mistake, e.g. incorrect module name.
            // Throw OFE so post-boot it only gets logged at DEBUG.
            throw ControllerMessages.MESSAGES.extensionModuleNotFound(e, module);
        } catch (ModuleLoadException e) {
            // The module is there but can't be loaded. Treat this as an internal problem.
            // Throw a runtime exception so it always gets logged at ERROR in the server log with stack trace details.
            throw ControllerMessages.MESSAGES.extensionModuleLoadingFailure(e, module);
        }
    }

}
