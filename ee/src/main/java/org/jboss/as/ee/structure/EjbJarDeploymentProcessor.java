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
package org.jboss.as.ee.structure;

import java.util.List;

import javax.annotation.ManagedBean;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentType;
import org.jboss.as.server.deployment.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ResourceRootType;
import org.jboss.as.server.deployment.ResourceRootTypeMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VirtualFile;

/**
 * Processor that only runs for ear deployments where no application.xml is provided. It examines jars in the ear to determine
 * which are EJB sub-deployments.
 *
 * TODO: Move this to the EJB subsystem.
 *
 * @author Stuart Douglas
 *
 */
public class EjbJarDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName STATELESS = DotName.createSimple("javax.ejb.Stateless");
    private static final DotName STATEFULL = DotName.createSimple("javax.ejb.Stateful");
    private static final DotName MESSAGE_DRIVEN = DotName.createSimple("javax.ejb.MessageDriven");
    private static final DotName SINGLETON = DotName.createSimple("javax.ejb.Singleton");
    private static final DotName MANAGED_BEAN_ANNOTATION_NAME = DotName.createSimple(ManagedBean.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        // TODO: deal with application clients, we need the manifest information
        List<ResourceRoot> potentialSubDeployments = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : potentialSubDeployments) {
            if (ResourceRootTypeMarker.isModuleRoot(resourceRoot)) {
                // module roots cannot be ejb jars
                continue;
            }
            VirtualFile ejbJarFile = resourceRoot.getRoot().getChild("META-INF/ejb-jar.xml");
            if (ejbJarFile.exists()) {
                ResourceRootTypeMarker.isType(ResourceRootType.EJB_JAR, resourceRoot);
            } else {
                final Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                if (index != null) {
                    if(!index.getAnnotations(STATEFULL).isEmpty() ||
                            !index.getAnnotations(STATELESS).isEmpty() ||
                            !index.getAnnotations(MESSAGE_DRIVEN).isEmpty() ||
                            !index.getAnnotations(SINGLETON).isEmpty() ||
                            !index.getAnnotations(MANAGED_BEAN_ANNOTATION_NAME).isEmpty()) {
                        //this is an EJB deployment
                        //TODO: we need to mark EJB sub deployments so the sub deployers know they are EJB deployments
                        ResourceRootTypeMarker.isType(ResourceRootType.EJB_JAR, resourceRoot);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
