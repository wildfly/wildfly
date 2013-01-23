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

package org.jboss.as.ejb3.deployment.processors.dd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.ImplicitDependsOnMetaData;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

/**
 * @author Stuart Douglas
 */
public class ImplicitDependsOnDDProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarMetaData metaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (metaData == null || metaData.getAssemblyDescriptor() == null) {
            return;
        }
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        // fetch the container-interceptors
        final List<ImplicitDependsOnMetaData> implicitDependsOn = metaData.getAssemblyDescriptor().getAny(ImplicitDependsOnMetaData.class);
        if (implicitDependsOn == null || implicitDependsOn.isEmpty()) {
            return;
        }
        Boolean defaultSetting = null;
        Map<String, Boolean> perEjbSetting = new HashMap<String, Boolean>();
        for (final ImplicitDependsOnMetaData dependsOn : implicitDependsOn) {
            if (dependsOn.getEjbName().equals("*")) {
                defaultSetting = dependsOn.isEnabled();
            } else {
                perEjbSetting.put(dependsOn.getEjbName(), dependsOn.isEnabled());
            }
        }
        // Now process container interceptors for each EJB
        for (final ComponentDescription componentDescription : eeModuleDescription.getComponentDescriptions()) {
            if (!(componentDescription instanceof EJBComponentDescription)) {
                continue;
            }
            if (perEjbSetting.containsKey(componentDescription.getComponentName())) {
                componentDescription.setViewDependsOnStart(perEjbSetting.get(componentDescription.getComponentName()));
            } else if (defaultSetting != null) {
                componentDescription.setViewDependsOnStart(defaultSetting);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
