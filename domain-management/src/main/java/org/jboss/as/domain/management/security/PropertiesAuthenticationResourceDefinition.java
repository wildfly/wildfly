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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a management security realm's properties-file-based authentication resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PropertiesAuthenticationResourceDefinition extends PropertiesFileResourceDefinition {

    public static final SimpleAttributeDefinition PLAIN_TEXT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PLAIN_TEXT, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false)).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = { PATH, RELATIVE_TO, PLAIN_TEXT };

    public PropertiesAuthenticationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.PROPERTIES),
                ManagementDescription.getResourceDescriptionResolver("core.management.security-realm.authentication.properties"),
                new SecurityRealmChildAddHandler(true, ATTRIBUTE_DEFINITIONS), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    protected AttributeDefinition[] getAttributeDefinitions() {
        return ATTRIBUTE_DEFINITIONS;
    }


}
