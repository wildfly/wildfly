/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.Attachable;
import org.jboss.msc.service.ServiceRegistry;

/**
 * The deployment unit.  This object retains data which is persistent for the life of the
 * deployment.
 */
public interface DeploymentUnit extends Attachable {

    /**
     * Get the deployment unit of the parent (enclosing) deployment.
     *
     * @return the parent deployment unit, or {@code null} if this is a top-level deployment
     */
    DeploymentUnit getParent();

    /**
     * Get the simple name of the deployment unit.
     *
     * @return the simple name
     */
    String getName();

    /**
     * Get the service registry.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();
}
