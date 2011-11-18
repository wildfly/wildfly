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

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;
import static org.jboss.wsf.spi.deployment.DeploymentType.JAXWS;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXWS_JSE;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * Creates new JAXWS JMS deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_JMS extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXWS_JMS() {
        super(JAXWS, JAXWS_JSE);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        // propagate
        final JMSEndpointsMetaData jmsEndpointsMD = getOptionalAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        dep.addAttachment(JMSEndpointsMetaData.class, jmsEndpointsMD);

        ROOT_LOGGER.creatingEndpointsMetaDataModel("JAXWS", "JMS");
        for (final JMSEndpointMetaData jmsEndpoint : jmsEndpointsMD.getEndpointsMetaData()) {
            final String jmsEndpointName = jmsEndpoint.getName();
            ROOT_LOGGER.jmsName(jmsEndpointName);
            final String jmsEndpointClassName = jmsEndpoint.getImplementor();
            ROOT_LOGGER.jmsClass(jmsEndpointClassName);
            final String jmsEndpointAddress = jmsEndpoint.getSoapAddress();
            ROOT_LOGGER.jmsAddress(jmsEndpointAddress);
            newJMSEndpoint(jmsEndpointClassName, jmsEndpointName, jmsEndpointAddress, dep);
        }
    }

}
