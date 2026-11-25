/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILESYSTEM_PATH;
import static org.jboss.as.ee.subsystem.EeCapabilities.EE_GLOBAL_DIRECTORY_CAPABILITY;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;

/**
 * The resource definition for global-directory child resource in EE subsystem.
 *
 * @author Yeray Borges
 */
public class GlobalDirectoryResourceDefinition extends PersistentResourceDefinition {

    static SimpleAttributeDefinition PATH = create(ModelDescriptionConstants.PATH, ModelType.STRING, false)
            .setAllowExpression(true)
            .addArbitraryDescriptor(FILESYSTEM_PATH, new ModelNode(true))
            .setRestartAllServices()
            .build();

    static SimpleAttributeDefinition RELATIVE_TO = create(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = new SimpleAttributeDefinition[]{PATH, RELATIVE_TO};

    private static final AbstractAddStepHandler ADD = new GlobalDirectoryAddHandler();
    private static final AbstractRemoveStepHandler REMOVE = ReloadRequiredRemoveStepHandler.INSTANCE;

    GlobalDirectoryResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(EESubsystemModel.GLOBAL_DIRECTORY), EeExtension.getResourceDescriptionResolver(EESubsystemModel.GLOBAL_DIRECTORY))
                .setAddHandler(GlobalDirectoryResourceDefinition.ADD)
                .setRemoveHandler(GlobalDirectoryResourceDefinition.REMOVE)
                .setCapabilities(EE_GLOBAL_DIRECTORY_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }


    private static class GlobalDirectoryAddHandler extends AbstractAddStepHandler {

        public GlobalDirectoryAddHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource)
                throws OperationFailedException {
            super.populateModel(context, operation, resource);
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext oc, ModelNode op) throws OperationFailedException {
                    Resource parentResource = context.readResourceFromRoot(context.getCurrentAddress().getParent(), false);
                    Set<String> globalDirectories = parentResource.getChildrenNames(EESubsystemModel.GLOBAL_DIRECTORY);

                    String newName = context.getCurrentAddressValue();
                    String firstAdded = !globalDirectories.isEmpty() ? globalDirectories.iterator().next() : null;
                    if (firstAdded != null && !firstAdded.equals(newName)) {
                        throw EeLogger.ROOT_LOGGER.oneGlobalDirectory(newName, firstAdded);
                    }
                }
            }, MODEL);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final String path = PATH.resolveModelAttribute(context, model).asString();
            final String relativeTo = RELATIVE_TO.resolveModelAttribute(context, model).asStringOrNull();

            final CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget()
                    .addCapability(EE_GLOBAL_DIRECTORY_CAPABILITY);
            final Consumer<GlobalDirectory> provides = serviceBuilder.provides(EE_GLOBAL_DIRECTORY_CAPABILITY);
            final Supplier<PathManager> pathManagerSupplier = serviceBuilder.requires(PathManager.SERVICE_DESCRIPTOR);

            Service globalDirectoryService = new GlobalDirectoryService(pathManagerSupplier, provides, context.getCurrentAddressValue(), path, relativeTo);

            serviceBuilder.setInstance(globalDirectoryService)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, handler);
        }
    }

    public static final class GlobalDirectory {
        private final Path resolvedPath;
        private final String name;

        public GlobalDirectory(Path resolvedPath, String name) {
            this.resolvedPath = resolvedPath;
            this.name = name;
        }

        public Path getResolvedPath() {
            return resolvedPath;
        }

        public String getName() {
            return name;
        }

        public String getModuleName() {
            return "global-directory." + name;
        }
    }
}
