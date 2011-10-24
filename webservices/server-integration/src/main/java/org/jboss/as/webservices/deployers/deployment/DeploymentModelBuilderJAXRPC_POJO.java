/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.util.ASHelper.getEndpointClassName;
import static org.jboss.as.webservices.util.ASHelper.getJBossWebMetaData;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.ASHelper.getServletForName;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICES_METADATA_KEY;
import static org.jboss.wsf.spi.deployment.DeploymentType.JAXRPC;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXRPC_JSE;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Creates new JAXRPC JSE deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXRPC_POJO extends AbstractDeploymentModelBuilder {

    /**
     * Constructor.
     */
    DeploymentModelBuilderJAXRPC_POJO() {
        super(JAXRPC, JAXRPC_JSE);
    }

    /**
     * Creates new JAXRPC JSE deployment and registers it with deployment unit.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        // propagate
        final JBossWebMetaData webMetaData = getJBossWebMetaData(unit);
        dep.addAttachment(JBossWebMetaData.class, webMetaData);
        // propagate
        final WebservicesMetaData wsMetaData = getOptionalAttachment(unit, WEBSERVICES_METADATA_KEY);
        dep.addAttachment(WebservicesMetaData.class, wsMetaData);

        log.debug("Creating JAXRPC POJO endpoints meta data model");
        for (final WebserviceDescriptionMetaData wsDescriptionMD : wsMetaData.getWebserviceDescriptions()) {
            for (final PortComponentMetaData portCompomentMD : wsDescriptionMD.getPortComponents()) {
                final String pojoEndpointName = portCompomentMD.getServletLink();
                log.debug("POJO name: " + pojoEndpointName);
                final ServletMetaData servletMD = getServletForName(webMetaData, pojoEndpointName);
                final String pojoEndpointClassName = getEndpointClassName(servletMD);
                log.debug("POJO class: " + pojoEndpointClassName);

                newHttpEndpoint(pojoEndpointClassName, pojoEndpointName, dep);
            }
        }
    }

}
