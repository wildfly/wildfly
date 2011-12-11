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

package org.jboss.as.management.client.content;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.BytesValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a resource that represents a named bit of re-usable DMR.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagedDMRContentResourceDefinition extends SimpleResourceDefinition {

    public static final AttributeDefinition HASH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HASH,
            ModelType.BYTES, false).setValidator(BytesValidator.createSha1(false)).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

    private final AttributeDefinition contentDefinition;

    public static ManagedDMRContentResourceDefinition create(final String childType,
                                               final ParameterValidator contentValidator,
                                               final ResourceDescriptionResolver descriptionResolver) {
        return new ManagedDMRContentResourceDefinition(childType, getContentAttributeDefinition(contentValidator), descriptionResolver);
    }

    private ManagedDMRContentResourceDefinition(final String childType,
                                               final AttributeDefinition contentDefinition,
                                               final ResourceDescriptionResolver descriptionResolver) {
        super(PathElement.pathElement(childType), descriptionResolver, new ManagedDMRContentAddHandler(contentDefinition, descriptionResolver),
                ManagedDMRContentRemoveHandler.INSTANCE, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
        this.contentDefinition = contentDefinition;
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(HASH, null);
        resourceRegistration.registerReadWriteAttribute(contentDefinition, null, new ManagedDMRContentWriteAttributeHandler(contentDefinition));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        final ManagedDMRContentStoreHandler handler = new ManagedDMRContentStoreHandler(contentDefinition, getResourceDescriptionResolver());
        resourceRegistration.registerOperationHandler(ManagedDMRContentStoreHandler.OPERATION_NAME, handler, handler);
    }

    private static AttributeDefinition getContentAttributeDefinition(final ParameterValidator contentValidator) {
        return SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONTENT, ModelType.OBJECT)
                .setValidator(contentValidator).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    }
}
