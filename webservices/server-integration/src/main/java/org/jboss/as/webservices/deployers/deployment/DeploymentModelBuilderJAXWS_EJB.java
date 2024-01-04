/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.metadata.model.AbstractEndpoint.COMPONENT_VIEW_NAME;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXWS_EJB3;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.deployers.WSEndpointConfigMapping;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates new JAXWS Enterprise Beans 3 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_EJB extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXWS_EJB() {
        super(JAXWS_EJB3);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        WSLogger.ROOT_LOGGER.trace("Creating JAXWS EJB endpoints meta data model");
        WSEndpointConfigMapping ecm = unit.getAttachment(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY);
        for (final EJBEndpoint ejbEndpoint : getJaxwsEjbs(unit)) {
            final String ejbEndpointName = ejbEndpoint.getName();
            WSLogger.ROOT_LOGGER.tracef("EJB name: %s", ejbEndpointName);
            final String ejbEndpointClassName = ejbEndpoint.getClassName();
            WSLogger.ROOT_LOGGER.tracef("EJB class: %s", ejbEndpointClassName);
            final Endpoint ep = newHttpEndpoint(ejbEndpointClassName, ejbEndpointName, dep);
            final ServiceName componentViewName = ejbEndpoint.getComponentViewName();
            if (componentViewName != null) {
                ep.setProperty(COMPONENT_VIEW_NAME, componentViewName);
            }
            ep.setEndpointConfig(ecm.getConfig(ejbEndpointClassName));
            markWeldDeployment(unit, ep);
        }
    }

}
