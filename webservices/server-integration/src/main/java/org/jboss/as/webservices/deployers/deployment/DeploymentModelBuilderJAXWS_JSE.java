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
package org.jboss.as.webservices.deployers.deployment;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * Creates new JAXWS JSE deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_JSE extends AbstractDeploymentModelBuilder {
    /**
     * Constructor.
     */
    DeploymentModelBuilderJAXWS_JSE() {
        super();
    }

    /**
     * Creates new JAXWS JSE deployment and registers it with deployment unit.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        this.log.debug("Creating JAXWS JSE endpoints meta data model");
        final JBossWebMetaData webMetaData = ASHelper.getJBossWebMetaData(unit);
        if (webMetaData != null) {
            dep.addAttachment(JBossWebMetaData.class, webMetaData);
            final List<ServletMetaData> servlets = ASHelper.getJaxwsServlets(unit);
            for (ServletMetaData servlet : servlets) {
                final String servletName = servlet.getName();
                this.log.debug("JSE name: " + servletName);
                final String servletClass = ASHelper.getEndpointName(servlet);
                this.log.debug("JSE class: " + servletClass);

                this.newHttpEndpoint(servletClass, servletName, dep);
            }
        }

        final JMSEndpointsMetaData jmsEndpointsMD = ASHelper.getOptionalAttachment(unit, WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY);
        if (jmsEndpointsMD != null) {
            dep.addAttachment(JMSEndpointsMetaData.class, jmsEndpointsMD);
            for (JMSEndpointMetaData endpoint : jmsEndpointsMD.getEndpointsMetaData()) {
                if (endpoint.getName() == null) {
                    endpoint.setName(endpoint.getImplementor());
                }
                this.newJMSEndpoint(endpoint.getImplementor(), endpoint.getName(), endpoint.getSoapAddress(), dep);
            }
        }
    }
}
