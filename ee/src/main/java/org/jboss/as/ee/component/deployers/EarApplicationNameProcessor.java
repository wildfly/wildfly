/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ear.spec.EarMetaData;

/**
 * The Java EE6 spec and EJB3.1 spec contradict each other on what the "application-name" semantics are.
 * The Java EE6 spec says that in the absence of a (top level) .ear, the application-name is same as the
 * (top level) module name. So if a blah.jar is deployed, as per Java EE6 spec, both the module name and
 * application name are "blah". This is contradictory to the EJB3.1 spec (JNDI naming section) which says
 * that in the absence of a (top level) .ear, the application-name is null.
 * <p/>
 * This deployment processor, sets up the {@link Attachments#EAR_APPLICATION_NAME} attachment with the value
 * that's semantically equivalent to what the EJB3.1 spec expects.
 *
 * @author Jaikiran Pai
 */
public class EarApplicationNameProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String earApplicationName = this.getApplicationName(deploymentUnit);
        deploymentUnit.putAttachment(Attachments.EAR_APPLICATION_NAME, earApplicationName);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Returns the application name for the passed deployment. If the passed deployment isn't an .ear or doesn't belong
     * to a .ear, then this method returns null. Else it returns the application-name set in the application.xml of the .ear
     * or if that's not set, will return the .ear deployment unit name (stripped off the .ear suffix).
     *
     * @param deploymentUnit The deployment unit
     */
    private String getApplicationName(DeploymentUnit deploymentUnit) {
        final DeploymentUnit parentDU = deploymentUnit.getParent();
        if (parentDU == null) {
            final EarMetaData earMetaData = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (earMetaData != null) {
                final String overriddenAppName = earMetaData.getApplicationName();
                if (overriddenAppName == null) {
                    return this.getEarName(deploymentUnit);
                }
                return overriddenAppName;
            } else {
                return this.getEarName(deploymentUnit);
            }
        }
        // traverse to top level DU
        return this.getApplicationName(parentDU);
    }

    /**
     * Returns the name (stripped off the .ear suffix) of the passed <code>deploymentUnit</code>.
     * Returns null if the passed <code>deploymentUnit</code>'s name doesn't end with .ear suffix.
     *
     * @param deploymentUnit Deployment unit
     * @return
     */
    private String getEarName(final DeploymentUnit deploymentUnit) {
        final String duName = deploymentUnit.getName();
        if (duName.endsWith(".ear")) {
            return duName.substring(0, duName.length() - ".ear".length());
        }
        return null;
    }
}
