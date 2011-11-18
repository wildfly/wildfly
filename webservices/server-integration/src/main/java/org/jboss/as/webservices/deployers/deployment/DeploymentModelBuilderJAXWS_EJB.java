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
import static org.jboss.as.webservices.metadata.model.EJBEndpoint.EJB_COMPONENT_VIEW_NAME;
import static org.jboss.as.webservices.util.ASHelper.getJaxwsEjbs;
import static org.jboss.wsf.spi.deployment.DeploymentType.JAXWS;
import static org.jboss.wsf.spi.deployment.EndpointType.JAXWS_EJB3;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates new JAXWS EJB3 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_EJB extends AbstractDeploymentModelBuilder {

    DeploymentModelBuilderJAXWS_EJB() {
        super(JAXWS, JAXWS_EJB3);
    }

    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
        ROOT_LOGGER.creatingEndpointsMetaDataModel("JAXWS", "EJB");
        for (final EJBEndpoint ejbEndpoint : getJaxwsEjbs(unit)) {
            final String ejbEndpointName = ejbEndpoint.getName();
            ROOT_LOGGER.ejbName(ejbEndpointName);
            final String ejbEndpointClassName = ejbEndpoint.getClassName();
            ROOT_LOGGER.ejbClass(ejbEndpointClassName);
            final Endpoint ep = newHttpEndpoint(ejbEndpointClassName, ejbEndpointName, dep);
            ep.setProperty(EJB_COMPONENT_VIEW_NAME, ejbEndpoint.getComponentViewName());
        }
    }

}
