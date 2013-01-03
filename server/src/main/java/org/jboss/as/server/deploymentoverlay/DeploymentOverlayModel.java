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

package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayModel {

    private static final String RESOURCE_NAME = DeploymentOverlayModel.class.getPackage().getName() + ".LocalDescriptions";

    protected static final PathElement CONTENT_PATH = PathElement.pathElement(ModelDescriptionConstants.CONTENT);
    protected static final PathElement DEPLOYMENT_OVERRIDE_PATH = PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT_OVERLAY);
    protected static final PathElement DEPLOYMENT_OVERRIDE_DEPLOYMENT_PATH = PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, DeploymentOverlayModel.class.getClassLoader(), true, false);
    }
}
