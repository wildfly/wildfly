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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class FileStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);

    // attributes
    static final SimpleAttributeDefinition RELATIVE_PATH = new SimpleAttributeDefinitionBuilder(ModelKeys.PATH, ModelType.STRING, true)
            .setXmlName(Attribute.PATH.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO, ModelType.STRING, true)
            .setXmlName(Attribute.RELATIVE_TO.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(ServerEnvironment.SERVER_DATA_DIR))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { RELATIVE_TO, RELATIVE_PATH };

    // operations
    private static final OperationDefinition ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ADD, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.FILE_STORE))
            .setParameters(PARAMETERS)
            .addParameter(RELATIVE_TO)
            .addParameter(RELATIVE_PATH)
            .setAttributeResolver(InfinispanExtension.getResourceDescriptionResolver(ModelKeys.FILE_STORE))
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, RELATIVE_PATH);
        }

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    private final ResolvePathHandler resolvePathHandler;

    FileStoreResourceDefinition(ResolvePathHandler resolvePathHandler, boolean allowRuntimeOnlyRegistration) {
        super(PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.FILE_STORE), new FileStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE, allowRuntimeOnlyRegistration);
        this.resolvePathHandler = resolvePathHandler;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    // override the add operation to provide a custom definition (for the optional PROPERTIES parameter to add())
    @Override
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(ADD_DEFINITION, handler);
        if (this.resolvePathHandler != null) {
            registration.registerOperationHandler(this.resolvePathHandler.getOperationDefinition(), this.resolvePathHandler);
        }
    }
}
