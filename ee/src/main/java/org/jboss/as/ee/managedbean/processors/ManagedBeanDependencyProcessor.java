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

package org.jboss.as.ee.managedbean.processors;

import java.util.List;

import javax.annotation.ManagedBean;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Deployment processor which adds a module dependencies for modules needed for managed bean deployments.
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 */
public class ManagedBeanDependencyProcessor implements DeploymentUnitProcessor {

    private static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());
    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");

    /**
     * Add dependencies for modules required for manged bean deployments, if managed bean configurations are attached
     * to the deployment.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            return;
        }
        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME);
        if (instances == null || instances.isEmpty()) {
            return;
        }

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        moduleSpecification.addDependency(new ModuleDependency(moduleLoader, JAVAEE_API_ID, false, false, false));
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
