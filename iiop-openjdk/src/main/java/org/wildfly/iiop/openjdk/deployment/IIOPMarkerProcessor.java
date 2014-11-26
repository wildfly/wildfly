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
package org.wildfly.iiop.openjdk.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.iiop.openjdk.rmi.ExceptionAnalysis;
import org.wildfly.iiop.openjdk.rmi.InterfaceAnalysis;
import org.wildfly.iiop.openjdk.rmi.ValueAnalysis;

/**
 * Processor responsible for marking a deployment as using IIOP
 *
 * @author Stuart Douglas
 */
public class IIOPMarkerProcessor implements DeploymentUnitProcessor{
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        //for now we mark all subsystems as using IIOP if the IIOP subsystem is installed
        IIOPDeploymentMarker.mark(deploymentUnit);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        //clear data from the relevant caches
        Module module = context.getAttachment(Attachments.MODULE);
        if(module == null) {
            return;
        }
        ExceptionAnalysis.clearCache(module.getClassLoader());
        InterfaceAnalysis.clearCache(module.getClassLoader());
        ValueAnalysis.clearCache(module.getClassLoader());
    }

}
