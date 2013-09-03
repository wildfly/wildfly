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

package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE_LOADING;

import java.io.File;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.LocalModuleFinder;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.NativeLibraryResourceLoader;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;

/**
 * Definition of the core-service=module-loading resource.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ModuleLoadingResourceDefinition extends SimpleResourceDefinition {

    private static final AttributeDefinition MODULE_NAME = new SimpleAttributeDefinitionBuilder(MODULE, ModelType.STRING).build();

    private final List<AccessConstraintDefinition> accessConstraints;

    public ModuleLoadingResourceDefinition() {
        super(PathElement.pathElement(CORE_SERVICE, MODULE_LOADING),
                ServerDescriptions.getResourceDescriptionResolver("core", MODULE_LOADING));
        this.accessConstraints = SensitiveTargetAccessConstraintDefinition.MODULE_LOADING.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        AttributeDefinition ad = SimpleListAttributeDefinition.Builder.of("module-roots",
                new SimpleAttributeDefinitionBuilder("module-root", ModelType.STRING).build())
                .setStorageRuntime()
                .setDeprecated(ModelVersion.create(1, 4, 0))
                .build();
        resourceRegistration.registerReadOnlyAttribute(ad, new ListModuleRootsHandler());
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {

        super.registerOperations(resourceRegistration);

        final OperationDefinition definition = new SimpleOperationDefinitionBuilder("list-resource-loader-paths", getResourceDescriptionResolver())
                .addParameter(MODULE_NAME)
                .setRuntimeOnly()
                .setReplyType(ModelType.LIST)
                .setReplyValueType(ModelType.STRING)
                .setDeprecated(ModelVersion.create(1, 4, 0))
                .setReadOnly()
                .build();

         resourceRegistration.registerOperationHandler(definition, new ModuleLocationHandler());
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    /** Read attribute handler for "module-roots" */
    private static class ListModuleRootsHandler extends AbstractRuntimeOnlyHandler {

        @Override
        protected boolean resourceMustExist(OperationContext context, ModelNode operation) {
            return false;
        }

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNode list = context.getResult().setEmptyList();

            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws Exception {
                        storeRepoRoots(list);
                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                throw new IllegalStateException(e.getCause());
            }

            context.stepCompleted();
        }

        private static void storeRepoRoots(final ModelNode list) throws NoSuchFieldException, IllegalAccessException {
            // TODO get a formal API from jboss-modules to replace this reflection
            ModuleLoader loader = Module.getBootModuleLoader();
            if (loader instanceof LocalModuleLoader) {
                LocalModuleLoader lml = (LocalModuleLoader) loader;
                Field findersField = ModuleLoader.class.getDeclaredField("finders");
                Field repoRootsField = null;
                findersField.setAccessible(true);
                try {
                    Object[] finders = (Object[]) findersField.get(lml);
                    if (finders.length > 0 && finders[0] instanceof LocalModuleFinder) {
                        LocalModuleFinder lmf = (LocalModuleFinder) finders[0];
                        repoRootsField = LocalModuleFinder.class.getDeclaredField("repoRoots") ;
                        repoRootsField.setAccessible(true);
                        File[] repoRoots = (File[]) repoRootsField.get(lmf);
                        for (File file : repoRoots) {
                            list.add(file.getAbsolutePath());
                        }
                    }
                } finally {
                    findersField.setAccessible(false);
                    if (repoRootsField != null) {
                        repoRootsField.setAccessible(false);
                    }
                }
            }

        }
    }

    /** Handler for the "list-resource-loader-paths" operation */
    private static final class ModuleLocationHandler implements OperationStepHandler {

        /** {@inheritDoc} */
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    final String moduleName = MODULE_NAME.resolveModelAttribute(context, operation).asString();

                    try {
                        List<String> paths = AccessController.doPrivileged(new PrivilegedExceptionAction<List<String>>() {
                            @Override
                            public List<String> run() throws Exception {
                                return findResourcePaths(moduleName);
                            }
                        });

                        ModelNode list = context.getResult().setEmptyList();
                        for (String path : paths) {
                            list.add(path);
                        }

                    } catch (PrivilegedActionException e) {
                        throw new OperationFailedException(e.getCause());
                    }
                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);

            context.stepCompleted();
        }
    }

    private static List<String> findResourcePaths(String moduleName) throws ModuleLoadException, NoSuchFieldException, IllegalAccessException {

        // TODO get a formal API from jboss-modules to replace this reflection

        List<String> result = new ArrayList<String>();
        Module module = Module.getModuleFromCallerModuleLoader(ModuleIdentifier.fromString(moduleName));
        ModuleClassLoader mcl = module.getClassLoader();
        Field pathsField = ModuleClassLoader.class.getDeclaredField("paths");
        Field sourceListField = null;
        Field resourceLoaderField = null;
        pathsField.setAccessible(true);
        try {
            Object paths = pathsField.get(mcl);
            sourceListField = paths.getClass().getDeclaredField("sourceList");
            sourceListField.setAccessible(true);
            Object[] resourceLoaderSpecs = (Object[]) sourceListField.get(paths);
            resourceLoaderField = ResourceLoaderSpec.class.getDeclaredField("resourceLoader");
            resourceLoaderField.setAccessible(true);
            for (Object resourceLoaderSpec : resourceLoaderSpecs) {
                ResourceLoader resourceLoader = (ResourceLoader) resourceLoaderField.get(resourceLoaderSpec);
                String path = getResourceLoaderPath(resourceLoader);
                if (path != null) {
                    result.add(path);
                }
            }
            return result;
        } finally {
            pathsField.setAccessible(false);
            if (sourceListField != null) {
                sourceListField.setAccessible(false);
            }
            if (resourceLoaderField != null) {
                resourceLoaderField.setAccessible(false);
            }
        }

    }

    private static String getResourceLoaderPath(ResourceLoader resourceLoader) throws NoSuchFieldException, IllegalAccessException {

        if (resourceLoader instanceof NativeLibraryResourceLoader) {
            return ((NativeLibraryResourceLoader) resourceLoader).getRoot().getAbsolutePath();
        } else if (isInstanceOf(resourceLoader, "org.jboss.modules.JarFileResourceLoader")) {
            Field fileOfJarField = resourceLoader.getClass().getDeclaredField("fileOfJar");
            fileOfJarField.setAccessible(true);
            try {
                File fileOfJar = (File) fileOfJarField.get(resourceLoader);
                return fileOfJar.getAbsolutePath();
            } finally {
                fileOfJarField.setAccessible(false);
            }

        } else if (isInstanceOf(resourceLoader, "org.jboss.modules.FilteredResourceLoader")) {
            Field loaderField = resourceLoader.getClass().getDeclaredField("loader");
            loaderField.setAccessible(true);
            try {
                ResourceLoader loader = (ResourceLoader) loaderField.get(resourceLoader);
                return getResourceLoaderPath(loader);
            } finally {
                loaderField.setAccessible(false);
            }
        }

        return null;
    }

    private static boolean isInstanceOf(Object obj, String className) {
        Class clazz = obj.getClass();
        while (clazz != null) {
            if (clazz.getCanonicalName().equals(className)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
}
