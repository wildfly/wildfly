/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.management;

import java.io.File;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchResourceDefinition extends SimpleResourceDefinition {

    public static final String NAME = "patching";
    static final String RESOURCE_NAME = PatchResourceDefinition.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, NAME);

    private static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, PatchResourceDefinition.class.getClassLoader(), true, false);
    }

    static final AttributeDefinition VERSION = SimpleAttributeDefinitionBuilder.create("version", ModelType.STRING)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition CUMULATIVE = SimpleAttributeDefinitionBuilder.create(Constants.CUMULATIVE, ModelType.STRING)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition PATCHES = PrimitiveListAttributeDefinition.Builder.of(Constants.PATCHES, ModelType.STRING)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition MODULE_PATH = PrimitiveListAttributeDefinition.Builder.of("module-path", ModelType.STRING)
            .setStorageRuntime()
            .build();

    // Patch operation
    static final AttributeDefinition INPUT_STREAM_IDX_DEF = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.INPUT_STREAM_INDEX, ModelType.INT)
            .setDefaultValue(new ModelNode(0))
            .setAllowNull(true)
            .build();
    static final AttributeDefinition OVERRIDE_MODULES = SimpleAttributeDefinitionBuilder.create(Constants.OVERRIDE_MODULES, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();
    static final AttributeDefinition OVERRIDE_ALL = SimpleAttributeDefinitionBuilder.create(Constants.OVERRIDE_ALL, ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();
    static final AttributeDefinition OVERRIDE = PrimitiveListAttributeDefinition.Builder.of(Constants.OVERRIDE, ModelType.STRING)
            .setAllowNull(true)
            .build();
    static final AttributeDefinition PRESERVE = PrimitiveListAttributeDefinition.Builder.of(Constants.PRESERVE, ModelType.STRING)
            .setAllowNull(true)
            .build();

    static final OperationDefinition PATCH = new SimpleOperationDefinitionBuilder(Constants.PATCH, getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .addParameter(INPUT_STREAM_IDX_DEF)
            .addParameter(OVERRIDE_ALL)
            .addParameter(OVERRIDE_MODULES)
            .addParameter(OVERRIDE)
            .addParameter(PRESERVE)
            .build();

    static final AttributeDefinition PATCH_ID = SimpleAttributeDefinitionBuilder.create(Constants.PATCH_ID, ModelType.STRING)
            .build();

    static final AttributeDefinition RESTORE_CONFIGURATION = SimpleAttributeDefinitionBuilder.create("restore-configuration", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setAllowNull(true)
            .build();

    static final AttributeDefinition ROLLBACK_TO = SimpleAttributeDefinitionBuilder.create("rollback-to", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();

    static final OperationDefinition ROLLBACK = new SimpleOperationDefinitionBuilder(Constants.ROLLBACK, getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .addParameter(PATCH_ID)
            .addParameter(ROLLBACK_TO)
            .addParameter(RESTORE_CONFIGURATION)
            .addParameter(OVERRIDE_ALL)
            .addParameter(OVERRIDE_MODULES)
            .addParameter(OVERRIDE)
            .addParameter(PRESERVE)
            .build();

    static final OperationDefinition SHOW_HISTORY = new SimpleOperationDefinitionBuilder("show-history", getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .build();

    static final ResourceDefinition INSTANCE = new PatchResourceDefinition();

    private PatchResourceDefinition() {
        super(PATH, getResourceDescriptionResolver(NAME));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        registry.registerReadOnlyAttribute(VERSION, new PatchAttributeReadHandler() {
            @Override
            void handle(ModelNode result, PatchInfo info) {
                result.set(info.getVersion());
            }
        });
        registry.registerReadOnlyAttribute(CUMULATIVE, new PatchAttributeReadHandler() {
            @Override
            void handle(ModelNode result, PatchInfo info) {
                result.set(info.getCumulativeID());
            }
        });
        registry.registerReadOnlyAttribute(PATCHES, new PatchAttributeReadHandler() {
            @Override
            void handle(ModelNode result, PatchInfo info) {
                result.setEmptyList();
                for(final String id : info.getPatchIDs()) {
                    result.add(id);
                }
            }
        });
        registry.registerReadOnlyAttribute(MODULE_PATH, new PatchAttributeReadHandler() {
            @Override
            void handle(ModelNode result, PatchInfo info) {
                result.setEmptyList();
                for(final File id : info.getModulePath()) {
                    result.add(id.getAbsolutePath());
                }
            }
        });
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        registry.registerOperationHandler(ROLLBACK, LocalPatchRollbackHandler.INSTANCE);
        registry.registerOperationHandler(PATCH, LocalPatchOperationStepHandler.INSTANCE);
        registry.registerOperationHandler(SHOW_HISTORY, LocalShowHistoryHandler.INSTANCE);
    }
}

