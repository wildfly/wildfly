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

package org.wildfly.extension.security.manager;

import static org.wildfly.extension.security.manager.Constants.MAXIMUM_SET;
import static org.wildfly.extension.security.manager.Constants.MINIMUM_SET;
import static org.wildfly.extension.security.manager.Constants.PERMISSION;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_ACTIONS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_CLASS;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_MODULE;
import static org.wildfly.extension.security.manager.Constants.PERMISSION_NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * Defines a resource that represents the security permissions that can be assigned to deployments.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class DeploymentPermissionsResourceDefinition extends PersistentResourceDefinition {


    static final SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(PERMISSION_CLASS, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(PERMISSION_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ACTIONS = new SimpleAttributeDefinitionBuilder(PERMISSION_ACTIONS, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(PERMISSION_MODULE, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    private static final ObjectTypeAttributeDefinition PERMISSIONS_VALUE_TYPE =
            ObjectTypeAttributeDefinition.Builder.of(PERMISSION, CLASS, NAME, ACTIONS, MODULE).build();

    static final AttributeDefinition MAXIMUM_PERMISSIONS =
            ObjectListAttributeDefinition.Builder.of(Constants.MAXIMUM_PERMISSIONS, PERMISSIONS_VALUE_TYPE)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setXmlName(MAXIMUM_SET)
                    .build();

    static final AttributeDefinition MINIMUM_PERMISSIONS =
            ObjectListAttributeDefinition.Builder.of(Constants.MINIMUM_PERMISSIONS, PERMISSIONS_VALUE_TYPE)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setXmlName(MINIMUM_SET)
                    .build();

    static final PathElement DEPLOYMENT_PERMISSIONS_PATH = PathElement.pathElement(
            Constants.DEPLOYMENT_PERMISSIONS, Constants.DEFAULT_VALUE);

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{MINIMUM_PERMISSIONS, MAXIMUM_PERMISSIONS};

    static final DeploymentPermissionsResourceDefinition INSTANCE = new DeploymentPermissionsResourceDefinition();

    private DeploymentPermissionsResourceDefinition() {
        super(DEPLOYMENT_PERMISSIONS_PATH, SecurityManagerExtension.getResolver(Constants.DEPLOYMENT_PERMISSIONS),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(Arrays.asList(ATTRIBUTES));
    }
}
