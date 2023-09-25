/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXWS_JSE;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * Creates new JAXWS Jakarta Messaging deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_JMS extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXWS_JMS() {
        super(JAXWS_JSE);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        // propagate
        final JMSEndpointsMetaData jmsEndpointsMD = getOptionalAttachment(unit, JMS_ENDPOINT_METADATA_KEY);
        dep.addAttachment(JMSEndpointsMetaData.class, jmsEndpointsMD);

        WSLogger.ROOT_LOGGER.trace("Creating JAXWS Jakarta Messaging endpoints meta data model");
        for (final JMSEndpointMetaData jmsEndpoint : jmsEndpointsMD.getEndpointsMetaData()) {
            final String jmsEndpointName = jmsEndpoint.getName();
            WSLogger.ROOT_LOGGER.tracef("Jakarta Messaging name: %s", jmsEndpointName);
            final String jmsEndpointClassName = jmsEndpoint.getImplementor();
            WSLogger.ROOT_LOGGER.tracef("Jakarta Messaging class: %s", jmsEndpointClassName);
            final String jmsEndpointAddress = jmsEndpoint.getSoapAddress();
            WSLogger.ROOT_LOGGER.tracef("Jakarta Messaging address: %s", jmsEndpointAddress);
            Endpoint ep =newJMSEndpoint(jmsEndpointClassName, jmsEndpointName, jmsEndpointAddress, dep);
            markWeldDeployment(unit, ep);
        }
    }

}
