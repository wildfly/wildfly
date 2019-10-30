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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;
import java.util.Map;

import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.ApplicationExceptionDescriptions;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.tx.ApplicationExceptionDetails;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.metadata.ejb.spec.ApplicationExceptionMetaData;
import org.jboss.metadata.ejb.spec.ApplicationExceptionsMetaData;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.modules.Module;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * @author Stuart Douglas
 */
public class ApplicationExceptionMergingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
            return;
        }
        final List<DeploymentUnit> accessibleSubDeployments = deploymentUnit.getAttachment(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ApplicationExceptions applicationExceptions = new ApplicationExceptions();
        for (DeploymentUnit unit : accessibleSubDeployments) {

            //we want to get the details for classes from all sub deployments we have access to
            final ApplicationExceptionDescriptions exceptionDescriptions = unit.getAttachment(EjbDeploymentAttachmentKeys.APPLICATION_EXCEPTION_DESCRIPTIONS);
            if (exceptionDescriptions != null) {
                for (Map.Entry<String, org.jboss.as.ejb3.tx.ApplicationExceptionDetails> exception : exceptionDescriptions.getApplicationExceptions().entrySet()) {
                    try {
                        final Class<?> index = ClassLoadingUtils.loadClass(exception.getKey(), module);
                        applicationExceptions.addApplicationException(index, exception.getValue());
                    } catch (ClassNotFoundException e) {
                        ROOT_LOGGER.debug("Could not load application exception class", e);
                    }
                }
            }
        }

        //now add the exceptions from the assembly descriptor
        EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData != null) {
            // process assembly-descriptor stuff
            AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetaData.getAssemblyDescriptor();
            if (assemblyDescriptor != null) {
                // process application-exception(s)
                ApplicationExceptionsMetaData ddAppExceptions = assemblyDescriptor.getApplicationExceptions();
                if (ddAppExceptions != null && !ddAppExceptions.isEmpty()) {
                    for (ApplicationExceptionMetaData applicationException : ddAppExceptions) {
                        String exceptionClassName = applicationException.getExceptionClass();
                        try {
                            final Class<?> index = ClassLoadingUtils.loadClass(exceptionClassName, module);
                            boolean rollback = applicationException.isRollback();
                            // by default inherited is true
                            boolean inherited = applicationException.isInherited() == null ? true : applicationException.isInherited();
                            // add the application exception to the ejb jar description
                            applicationExceptions.addApplicationException(index, new ApplicationExceptionDetails(exceptionClassName, inherited, rollback));
                        } catch (ClassNotFoundException e) {
                            throw EjbLogger.ROOT_LOGGER.failToLoadAppExceptionClassInEjbJarXml(exceptionClassName, e);
                        }
                    }
                }
            }
        }
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.APPLICATION_EXCEPTION_DETAILS, applicationExceptions);

    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
