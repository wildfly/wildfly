/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.webservices.util.ASHelper.getJaxrpcDeployment;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.ASHelper.getRequiredAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICES_METADATA_KEY;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXRPCDeployment;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSIntegrationProcessorJAXRPC_EJB implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final EjbJarMetaData ejbJarMD = getOptionalAttachment(unit, EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        final WebservicesMetaData webservicesMD = getOptionalAttachment(unit, WEBSERVICES_METADATA_KEY);
        if (ejbJarMD != null && webservicesMD != null) {
            processServiceEndpoints(unit, ejbJarMD, webservicesMD);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // does nothing
    }

    private static void processServiceEndpoints(final DeploymentUnit unit, final EjbJarMetaData ejbJarMD, final WebservicesMetaData webservicesMD) {
        final EEModuleDescription moduleDescription = getRequiredAttachment(unit, EE_MODULE_DESCRIPTION);
        final JAXRPCDeployment jaxrpcDeployment = getJaxrpcDeployment(unit);

        for (final WebserviceDescriptionMetaData webserviceDescriptionMD : webservicesMD.getWebserviceDescriptions()) {
            for (final PortComponentMetaData portComponentMD : webserviceDescriptionMD.getPortComponents()) {
                final String ejbName = portComponentMD.getEjbLink();
                final SessionBeanComponentDescription sessionBean = (SessionBeanComponentDescription)moduleDescription.getComponentByName(ejbName);
                final String seiIfaceClassName = portComponentMD.getServiceEndpointInterface();
                final EJBViewDescription ejbViewDescription = sessionBean.addWebserviceEndpointView(seiIfaceClassName);
                final String ejbViewName = ejbViewDescription.getServiceName().getCanonicalName();
                jaxrpcDeployment.addEndpoint(new EJBEndpoint(sessionBean, null, ejbViewName));
            }
        }
    }

}
