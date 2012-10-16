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

package org.jboss.as.patching;

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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchResourceDefinition extends SimpleResourceDefinition {

    static final String NAME = "patching";
    static final String RESOURCE_NAME = PatchResourceDefinition.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE, NAME);

    private static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, PatchResourceDefinition.class.getClassLoader(), true, false);
    }

    static final AttributeDefinition VERSION = SimpleAttributeDefinitionBuilder.create("version", ModelType.STRING)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition CUMULATIVE = SimpleAttributeDefinitionBuilder.create("cumulative", ModelType.STRING)
            .setStorageRuntime()
            .build();
    static final AttributeDefinition PATCHES = PrimitiveListAttributeDefinition.Builder.of("patches", ModelType.STRING)
            .setStorageRuntime()
            .build();

    static final OperationDefinition PATCH = new SimpleOperationDefinitionBuilder("patch", getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .build();

    static final OperationDefinition SHOW_HISTORY = new SimpleOperationDefinitionBuilder("show-history", getResourceDescriptionResolver(PatchResourceDefinition.NAME))
            .build();

    public static final ResourceDefinition INSTANCE = new PatchResourceDefinition();

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
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        registry.registerOperationHandler(PATCH, LocalPatchOperationStepHandler.INSTANCE);
        registry.registerOperationHandler(SHOW_HISTORY, LocalShowHistoryHandler.INSTANCE);
    }
}
