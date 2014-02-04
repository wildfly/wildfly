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

package org.jboss.as.controller.client.helpers.domain;

import org.jboss.as.controller.client.logging.ControllerClientLogger;

/**
 * {@link InvalidDeploymentPlanException} thrown when a deployment plan
 * specifies that a new version of content replace existing content of the same
 * unique name, but does not apply the replacement to all server groups that
 * have the existing content deployed.
 *
 * @author Brian Stansberry
 */
public class IncompleteDeploymentReplaceException extends InvalidDeploymentPlanException {

    private static final long serialVersionUID = -8322852398826927588L;

    public IncompleteDeploymentReplaceException(String deploymentName, String... missingGroups) {
        super(ControllerClientLogger.ROOT_LOGGER.incompleteDeploymentReplace(deploymentName, createMissingGroups(missingGroups)));
    }

    private static String createMissingGroups(String[] missingGroups) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < missingGroups.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(missingGroups[i]);
        }
        return sb.toString();
    }
}
