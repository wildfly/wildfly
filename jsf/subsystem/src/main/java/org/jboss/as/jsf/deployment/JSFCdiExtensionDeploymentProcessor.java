/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsf.deployment;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;

/**
 * {@link DeploymentUnitProcessor} which registers CDI extensions. Currently only registers
 * {@link JSFPassivatingViewScopedCdiExtension} to workaround WFLY-3044 / JAVASERVERFACES-3191.
 *
 * @author <a href="http://www.radoslavhusar.com/">Radoslav Husar</a>
 * @version Apr 10, 2014
 * @since 8.0.1
 */
public class JSFCdiExtensionDeploymentProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(parent);
            extensions.registerExtensionInstance(new JSFPassivatingViewScopedCdiExtension(), parent);
        }
    }

    public void undeploy(DeploymentUnit context) {
        // Do nothing.
    }
}