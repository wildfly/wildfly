/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health.deployment;

import io.smallrye.health.SmallRyeHealthReporter;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.wildfly.extension.microprofile.health.MicroProfileHealthSubsystemDefinition;

/**
 */
public class DeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            final SmallRyeHealthReporter healthReporter = (SmallRyeHealthReporter) phaseContext.getServiceRegistry().getService(MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_SERVICE).getValue();

            WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(deploymentUnit);
            extensions.registerExtensionInstance(new CDIExtension(healthReporter), deploymentUnit);
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}