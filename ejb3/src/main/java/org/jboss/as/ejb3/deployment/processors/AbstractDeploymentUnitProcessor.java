/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.deployment.processors;

import java.util.List;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * If this is an appclient we want to make the components as not installable, so we can still look up which EJB's are in
     * the deployment, but do not actual install them
     */
    protected final boolean appclient;

    protected AbstractDeploymentUnitProcessor(final boolean appclient) {
        this.appclient = appclient;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get hold of the deployment unit
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            EjbLogger.DEPLOYMENT_LOGGER.tracef("Skipping EJB annotation processing since no composite annotation index found in unit: %s", deploymentUnit);
        } else {
            if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
                EjbLogger.DEPLOYMENT_LOGGER.trace("Skipping EJB annotation processing due to deployment being metadata-complete. ");
            } else {
                processAnnotations(deploymentUnit, compositeIndex);
            }
        }
        processDeploymentDescriptor(deploymentUnit);
    }

    protected static EjbJarDescription getEjbJarDescription(final DeploymentUnit deploymentUnit) {
        EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        if (ejbJarDescription == null) {
            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            ejbJarDescription = new EjbJarDescription(moduleDescription, deploymentUnit.getName().endsWith(".war"));
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION, ejbJarDescription);
        }
        return ejbJarDescription;
    }

    protected void processAnnotations(final DeploymentUnit deploymentUnit, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        // do nothing
    }

    protected abstract void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData ejb) throws DeploymentUnitProcessingException;

    protected void processDeploymentDescriptor(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        // find the EJB jar metadata and start processing it
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        final SimpleSet<String> annotatedEJBs;
        if (appclient) {
            final List<ComponentDescription> additionalComponents = deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.ADDITIONAL_RESOLVABLE_COMPONENTS);
            annotatedEJBs = new SimpleSet<String>() {
                @Override
                public boolean contains(Object o) {
                    for (final ComponentDescription component : additionalComponents) {
                        if (component.getComponentName().equals(o)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        } else {
            final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);
            annotatedEJBs = new SimpleSet<String>() {
                @Override
                public boolean contains(Object o) {
                    return ejbJarDescription.hasComponent((String) o);
                }
            };
        }
        // process EJBs
        final EnterpriseBeansMetaData ejbs = ejbJarMetaData.getEnterpriseBeans();
        if (ejbs != null && !ejbs.isEmpty()) {
            for (final EnterpriseBeanMetaData ejb : ejbs) {
                final String beanName = ejb.getName();
                // the important bit is to skip already processed EJBs via annotations
                if (annotatedEJBs.contains(beanName)) {
                    continue;
                }

                processBeanMetaData(deploymentUnit, ejb);
            }
        }
        EjbDeploymentMarker.mark(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // do nothing
    }

    private interface SimpleSet<E> {
        boolean contains(Object o);
    }
}
