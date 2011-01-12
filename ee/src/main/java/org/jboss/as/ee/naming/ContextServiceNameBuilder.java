/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.naming;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Creates service names for ContextServices
 *
 * @author Stuart Douglas
 *
 */
public class ContextServiceNameBuilder {

    /**
     * Returns the service name for the java:app context of the deployment
     */
    public static ServiceName app(final DeploymentUnit deploymentUnit) {
        DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        return ContextNames.APPLICATION_CONTEXT_SERVICE_NAME.append(parent.getName());
    }

    /**
     * Returns the service name for the java:module context of the deployment
     */
    public static ServiceName module(final DeploymentUnit deploymentUnit) {
        DeploymentUnit parent = deploymentUnit.getParent();
        if(parent == null) {
            return ContextNames.MODULE_CONTEXT_SERVICE_NAME.append(deploymentUnit.getName());
        }
        return ContextNames.MODULE_CONTEXT_SERVICE_NAME.append(parent.getName()).append(deploymentUnit.getName());
    }

    private ContextServiceNameBuilder() {
    }

}
