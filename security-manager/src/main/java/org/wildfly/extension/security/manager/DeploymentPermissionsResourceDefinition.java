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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;

/**
 * Defines a resource that represents the security permissions that can be assigned to deployments.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
class DeploymentPermissionsResourceDefinition extends SimplePersistentResourceDefinition {

    static final PathElement DEPLOYMENT_PERMISSIONS_PATH = PathElement.pathElement(
            Constants.DEPLOYMENT_PERMISSIONS, Constants.DEFAULT_VALUE);

    static final DeploymentPermissionsResourceDefinition INSTANCE = new DeploymentPermissionsResourceDefinition();

    private static final List<? extends SimplePersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(
            Arrays.asList(new PermissionSetResourceDefinition(Constants.MINIMUM_SET),
                    new PermissionSetResourceDefinition(Constants.MAXIMUM_SET)));

    private DeploymentPermissionsResourceDefinition() {
        super(DEPLOYMENT_PERMISSIONS_PATH, SecurityManagerExtension.getResolver(Constants.DEPLOYMENT_PERMISSIONS),
                new AbstractAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    @Override
    public String getXmlElementName() {
        return Constants.DEPLOYMENT_PERMISSIONS;
    }
}
