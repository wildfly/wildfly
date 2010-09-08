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

package org.jboss.as.deployment.managedbean;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.ModuleIdentifier;

import javax.annotation.ManagedBean;

/**
 * Deployment processor which adds a module dependencies for modules needed for managed bean deployments.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDependencyProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.MODULE_DEPENDENCIES.plus(200L);
    private static ModuleIdentifier JAVAX_ANNOTATION_API_ID = ModuleIdentifier.create("javax.annotation.api");
    private static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());

    /**
     * Add dependencies for modules required for manged bean deployments, if managed bean configurations are attached
     * to the deployment.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final Index index = context.getAttachment(AnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }
        if(!index.getAnnotationTargets(MANAGED_BEAN_ANNOTATION_NAME).isEmpty()) {
            ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(JAVAX_ANNOTATION_API_ID, true, false, false));
        }
    }
}
