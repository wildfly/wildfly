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
import org.jboss.msc.service.ServiceTarget;

/**
 * The deployment unit processor context.  Maintains state pertaining to the current cycle
 * of deployment/undeployment.  This context object will be discarded when processing is
 * complete; data which must persist for the life of the deployment should be attached to
 * the {@link DeploymentUnit}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeploymentPhaseContext extends Attachable {

    /**
     * Get the service target into which this phase should install services.
     *
     * @return the service target
     */
    ServiceTarget getServiceTarget();

    /**
     * Get the service registry for the container, which may be used to look up services.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Get the persistent deployment unit context for this deployment unit.
     *
     * @return the deployment unit context
     */
    DeploymentUnit getDeploymentUnitContext();
}
