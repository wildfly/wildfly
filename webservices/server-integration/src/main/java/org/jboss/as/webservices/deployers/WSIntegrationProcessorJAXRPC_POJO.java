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

import static org.jboss.as.webservices.util.ASHelper.getEndpointClassName;
import static org.jboss.as.webservices.util.ASHelper.getJBossWebMetaData;
import static org.jboss.as.webservices.util.ASHelper.getJaxrpcDeployment;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.ASHelper.getServletForName;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICES_METADATA_KEY;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.JAXRPCDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSIntegrationProcessorJAXRPC_POJO implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (isJaxwsPojoDeployment(unit)) return;
        final JBossWebMetaData jbossWebMD = getJBossWebMetaData(unit);
        final WebservicesMetaData webservicesMD = getOptionalAttachment(unit, WEBSERVICES_METADATA_KEY);
        if (jbossWebMD != null && webservicesMD != null) {
            createJaxrpcDeployment(unit, webservicesMD, jbossWebMD);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // does nothing
    }

    private static boolean isJaxwsPojoDeployment(final DeploymentUnit unit) {
        return getJaxwsPojos(unit).size() > 0;
    }

    private static void createJaxrpcDeployment(final DeploymentUnit unit, final WebservicesMetaData webservicesMD, final JBossWebMetaData jbossWebMD) {
        final JAXRPCDeployment jaxrpcDeployment = getJaxrpcDeployment(unit);

        for (final WebserviceDescriptionMetaData wsDescriptionMD : webservicesMD.getWebserviceDescriptions()) {
            for (final PortComponentMetaData portComponentMD : wsDescriptionMD.getPortComponents()) {
                final POJOEndpoint pojoEndpoint = newPojoEndpoint(portComponentMD, jbossWebMD);
                jaxrpcDeployment.addEndpoint(pojoEndpoint);
            }
        }
    }

    private static POJOEndpoint newPojoEndpoint(final PortComponentMetaData portComponentMD, final JBossWebMetaData jbossWebMD) {
        final String endpointName = portComponentMD.getServletLink();
        final ServletMetaData servletMD = getServletForName(jbossWebMD, endpointName);
        final String endpointClassName = getEndpointClassName(servletMD);
        final String urlPattern = getUrlPattern(endpointName, jbossWebMD);

        return new POJOEndpoint(endpointName, endpointClassName, urlPattern);
    }

    private static String getUrlPattern(final String servletName, final JBossWebMetaData jbossWebMD) {
        for (final ServletMappingMetaData servletMappingMD : jbossWebMD.getServletMappings()) {
            if (servletName.equals(servletMappingMD.getServletName())) {
                return servletMappingMD.getUrlPatterns().get(0);
            }
        }
        throw new IllegalStateException();
    }

}
