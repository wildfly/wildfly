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

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.util.ASHelper.getJaxrpcPojos;
import static org.jboss.wsf.spi.deployment.DeploymentType.JAXRPC;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXRPC_JSE;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * Creates new JAXRPC POJO deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXRPC_POJO extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXRPC_POJO() {
        super(JAXRPC, JAXRPC_JSE);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        ROOT_LOGGER.creatingEndpointsMetaDataModel("JAXRPC", "POJO");
        for (final POJOEndpoint pojoEndpoint : getJaxrpcPojos(unit)) {
            final String pojoEndpointName = pojoEndpoint.getName();
            ROOT_LOGGER.pojoName(pojoEndpointName);
            final String pojoEndpointClassName = pojoEndpoint.getClassName();
            ROOT_LOGGER.pojoClass(pojoEndpointClassName);
            newHttpEndpoint(pojoEndpointClassName, pojoEndpointName, dep);
        }
    }

}
