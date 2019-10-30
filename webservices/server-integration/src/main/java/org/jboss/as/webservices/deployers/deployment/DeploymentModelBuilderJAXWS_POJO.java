/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
            }
        }
    }

}
