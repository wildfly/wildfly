/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class EJBComponentDescriptionFactory implements DeploymentUnitProcessor {
    private static final Logger logger = Logger.getLogger(EJBComponentDescriptionFactory.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get hold of the deployment unit
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        // TODO: metadata-complete
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Skipping EJB annotation processing since no composite annotation index found in unit: " + deploymentUnit);
            }
        } else {
            if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Skipping EJB annotation processing due to deployment being metadata-complete. ");
                }
            } else {
                processAnnotations(deploymentUnit, compositeIndex);
            }
        }
        processDeploymentDescriptor(deploymentUnit);
    }

    protected void processDeploymentDescriptor(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        // find the EJB jar metadata and start processing it
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        // process EJBs
        final EnterpriseBeansMetaData ejbs = ejbJarMetaData.getEnterpriseBeans();
        if (ejbs != null && !ejbs.isEmpty()) {
            for (final EnterpriseBeanMetaData ejb : ejbs) {
                processBeanMetaData(deploymentUnit, ejb);
            }
        }
        EjbDeploymentMarker.mark(deploymentUnit);
    }

    protected static EjbJarDescription getEjbJarDescription(final DeploymentUnit deploymentUnit) {
        EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        if (ejbJarDescription == null) {
            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            ejbJarDescription = new EjbJarDescription(moduleDescription, applicationClassesDescription, deploymentUnit.getName().endsWith(".war"));
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION, ejbJarDescription);
        }
        return ejbJarDescription;
    }

    static EnterpriseBeansMetaData getEnterpriseBeansMetaData(final DeploymentUnit deploymentUnit) {
        final EjbJarMetaData jarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (jarMetaData == null)
            return null;
        return jarMetaData.getEnterpriseBeans();
    }

    static EnterpriseBeanMetaData getEnterpriseBeanMetaData(final DeploymentUnit deploymentUnit, final String name) {
        final EnterpriseBeansMetaData enterpriseBeansMetaData = getEnterpriseBeansMetaData(deploymentUnit);
        if (enterpriseBeansMetaData == null)
            return null;
        return enterpriseBeansMetaData.get(name);
    }

    @Deprecated
    static <B extends EnterpriseBeanMetaData> B getEnterpriseBeanMetaData(final DeploymentUnit deploymentUnit, final String name, final Class<B> expectedType) {
        final EnterpriseBeansMetaData enterpriseBeansMetaData = getEnterpriseBeansMetaData(deploymentUnit);
        if (enterpriseBeansMetaData == null)
            return null;
        return expectedType.cast(enterpriseBeansMetaData.get(name));
    }

    /**
     * Process annotations and merge any available metadata at the same time.
     */
    protected abstract void processAnnotations(final DeploymentUnit deploymentUnit, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException;

    protected abstract void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData enterpriseBeanMetaData) throws DeploymentUnitProcessingException;

    protected static <T> T override(T original, T override) {
        return override != null ? override : original;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // do nothing
    }

}
