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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a management security realm references to property files.
 *
 * This abstract definition is shared by both the authentication and authorization definitions that depend on properties files.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class PropertiesFileResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH,
            ModelType.STRING, false).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(
            ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public PropertiesFileResourceDefinition(final PathElement pathElement,
            final ResourceDescriptionResolver descriptionResolver, final OperationStepHandler addHandler,
            final OperationStepHandler removeHandler, final OperationEntry.Flag addRestartLevel,
            final OperationEntry.Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(getAttributeDefinitions());
        handler.registerAttributes(resourceRegistration);
    }

    protected abstract AttributeDefinition[] getAttributeDefinitions();

}
