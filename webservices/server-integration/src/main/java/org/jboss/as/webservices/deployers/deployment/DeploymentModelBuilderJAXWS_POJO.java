/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.metadata.model.AbstractEndpoint.COMPONENT_VIEW_NAME;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXWS_JSE;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.deployers.WSEndpointConfigMapping;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates new JAXWS POJO deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_POJO extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXWS_POJO() {
        super(JAXWS_JSE);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        WSLogger.ROOT_LOGGER.trace("Creating JAXWS POJO endpoints meta data model");
        WSEndpointConfigMapping ecm = unit.getAttachment(WSAttachmentKeys.WS_ENDPOINT_CONFIG_MAPPING_KEY);
        for (final POJOEndpoint pojoEndpoint : getJaxwsPojos(unit)) {
            final String pojoEndpointName = pojoEndpoint.getName();
            WSLogger.ROOT_LOGGER.tracef("POJO name: %s", pojoEndpointName);
            final String pojoEndpointClassName = pojoEndpoint.getClassName();
            WSLogger.ROOT_LOGGER.tracef("POJO class: %s", pojoEndpointClassName);
            final Endpoint ep = newHttpEndpoint(pojoEndpointClassName, pojoEndpointName, dep);
            final ServiceName componentViewName = pojoEndpoint.getComponentViewName();
            if (componentViewName != null) {
                ep.setProperty(COMPONENT_VIEW_NAME, componentViewName);
            }
            if (ecm != null) {
                ep.setEndpointConfig(ecm.getConfig(pojoEndpointClassName));
                markWeldDeployment(unit, ep);
            }
        }
    }

}
