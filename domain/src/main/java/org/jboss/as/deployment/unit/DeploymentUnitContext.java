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
import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * The deployment unit context.  Instances of this interface are passed to each domain deployment on the server in order to serve
 * two purposes:
 * <ol>
 * <li>Allow for the addition of new deployment items which will be installed as a part of this deployment unit.</li>
 * <li>Provide a coordination point and type-safe storage location for the current processing state of the deployment.</li>
 * </ol>
 * Once deployment is complete, the deployment unit context need not be retained.
 */
public interface DeploymentUnitContext extends Attachable {

    /**
     * The service name under which all deployment unit services are registered.
     */
    ServiceName JBOSS_DEPLOYMENT_UNIT = ServiceName.JBOSS.append("deployment", "unit");

    /**
     * The service name under which all root controllers for deployment units are registered.
     */
    ServiceName JBOSS_DEPLOYMENT_UNIT_CONTROLLER = JBOSS_DEPLOYMENT_UNIT.append("controller");

    /**
     * Get the simple name of the deployment unit.
     *
     * @return the simple name
     */
    String getName();

    /**
     * Add a deployment item to this deployment unit context.  Once a deployment item is added, it may not be removed.
     *
     * @param item the item to add
     */
    void addDeploymentItem(DeploymentItem item);
}
