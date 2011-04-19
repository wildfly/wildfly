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

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

/**
 * Processes the assembly-descriptor section of a ejb-jar.xml of a EJB deployment and updates the {@link EjbJarDescription}
 * appropriately with this info.
 *
 * @author Jaikiran Pai
 */
public class AssemblyDescriptorProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get the deployment unit
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // find the EJB jar metadata and start processing it
        EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        // process assembly-descriptor stuff
        AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetaData.getAssemblyDescriptor();
        if (assemblyDescriptor != null) {
            // get hold of the ejb jar description (to which we'll be adding this assembly description metadata)
            final EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
            // process application-exception(s)
            ApplicationExceptionsMetaData applicationExceptions = assemblyDescriptor.getApplicationExceptions();
            if (applicationExceptions != null && !applicationExceptions.isEmpty()) {
                for (ApplicationExceptionMetaData applicationException : applicationExceptions) {
                    String exceptionClassName = applicationException.getExceptionClass();
                    boolean rollback = applicationException.isRollback();
                    // by default inherited is true
                    boolean inherited = applicationException.isInherited() == null ? true : applicationException.isInherited();
                    // add the application exception to the ejb jar description
                    ejbJarDescription.addApplicationException(exceptionClassName, rollback, inherited);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
