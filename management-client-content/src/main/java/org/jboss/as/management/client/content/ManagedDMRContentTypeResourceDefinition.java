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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a resource that represents the parent node for a tree of child resources each of
 * which represents a named bit of re-usable DMR.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagedDMRContentTypeResourceDefinition extends SimpleResourceDefinition {

    public static final AttributeDefinition HASH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HASH,
            ModelType.BYTES, true).setValidator(BytesValidator.createSha1(true)).build();

    public ManagedDMRContentTypeResourceDefinition(final ContentRepository contentRepository,
                                                   final String childType,
                                                   final PathElement pathElement,
                                                   final ResourceDescriptionResolver descriptionResolver) {
        super(pathElement, descriptionResolver, new ManagedDMRContentTypeAddHandler(contentRepository, childType),
                null, OperationEntry.Flag.RESTART_NONE, null);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(HASH, null);
    }
}
