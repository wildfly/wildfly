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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.AttachmentKeys;
import org.jboss.as.webservices.util.ASHelper;
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
final class DeploymentModelBuilderJAXRPC_JSE extends AbstractDeploymentModelBuilder {
    /**
     * Constructor.
     */
    DeploymentModelBuilderJAXRPC_JSE() {
        super();
    }

    /**
     * Creates new JAXRPC JSE deployment and registers it with deployment unit.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        final JBossWebMetaData webMetaData = ASHelper.getJBossWebMetaData(unit);
        if (webMetaData != null) {
            dep.addAttachment(JBossWebMetaData.class, webMetaData);
        }
        final WebservicesMetaData wsMetaData = ASHelper.getOptionalAttachment(unit, AttachmentKeys.WEBSERVICES_METADATA_KEY);
        if (wsMetaData != null) {
            dep.addAttachment(WebservicesMetaData.class, wsMetaData);
        }

        this.log.debug("Creating JAXRPC JSE endpoints meta data model");
        for (WebserviceDescriptionMetaData wsd : wsMetaData.getWebserviceDescriptions()) {
            for (PortComponentMetaData pcmd : wsd.getPortComponents()) {
                final String servletName = pcmd.getServletLink();
                this.log.debug("JSE name: " + servletName);
                final ServletMetaData servletMD = ASHelper.getServletForName(webMetaData, servletName);
                final String servletClass = ASHelper.getEndpointName(servletMD);
                this.log.debug("JSE class: " + servletClass);

                this.newHttpEndpoint(servletClass, servletName, dep);
            }
        }
    }
}
