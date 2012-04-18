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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.dmr.ModelType;

/**
 * The resource definition for the plug-in definition within both the authentication and authorization sections of the security
 * realm definition.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class AbstractPlugInAuthResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME,
            ModelType.STRING, false).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setAllowNull(false).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    AbstractPlugInAuthResourceDefinition(PathElement pathElement, ResourceDescriptionResolver resourceDescriptionResolver,
            SecurityRealmChildAddHandler securityRealmChildAddHandler,
            SecurityRealmChildRemoveHandler securityRealmChildRemoveHandler, Flag restartResourceServices,
            Flag restartResourceServices2) {
        super(pathElement, resourceDescriptionResolver, securityRealmChildAddHandler, securityRealmChildRemoveHandler,
                restartResourceServices, restartResourceServices2);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(NAME);
        handler.registerAttributes(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new PropertyResourceDefinition());
    }

}
