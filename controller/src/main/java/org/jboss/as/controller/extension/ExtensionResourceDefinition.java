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

package org.jboss.as.controller.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for an {@link Extension} resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MODULE, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1)).build();

    public ExtensionResourceDefinition(final ExtensionRegistry extensionRegistry, final boolean parallelBoot) {
        super(PathElement.pathElement(EXTENSION), CommonDescriptions.getResourceDescriptionResolver(EXTENSION, false),
                new ExtensionAddHandler(extensionRegistry, parallelBoot), new ExtensionRemoveHandler(extensionRegistry),
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(MODULE, null);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ExtensionSubsystemResourceDefinition());
    }
}
