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
import org.jboss.as.webservices.util.ASHelper;
//import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * Creates new JAXWS EJB3 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXWS_EJB3 extends AbstractDeploymentModelBuilder {
    /**
     * Constructor.
     */
    DeploymentModelBuilderJAXWS_EJB3() {
        super();
    }

    /**
     * Creates new JAXWS EJB3 deployment and registers it with deployment unit.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
//        this.getAndPropagateAttachment(WebServiceDeployment.class, unit, dep);
//        this.getAndPropagateAttachment(JBossMetaData.class, unit, dep);
//
//        this.log.debug("Creating JAXWS EJB3 endpoints meta data model");
//        for (final WebServiceDeclaration container : ASHelper.getJaxwsEjbs(unit)) {
//            final String ejbName = container.getComponentName();
//            this.log.debug("EJB3 name: " + ejbName);
//            final String ejbClass = container.getComponentClassName();
//            this.log.debug("EJB3 class: " + ejbClass);
//
//            final Endpoint ep = this.newHttpEndpoint(ejbClass, ejbName, dep);
//            ep.setProperty(ASHelper.CONTAINER_NAME, container.getContainerName());
//        }
    }
}
