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
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Creates new JAXRPC EJB21 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentModelBuilderJAXRPC_EJB21 extends AbstractDeploymentModelBuilder {
    /**
     * Constructor.
     */
    DeploymentModelBuilderJAXRPC_EJB21() {
        super();
    }

    /**
     * Creates new JAXRPC EJB21 deployment and registers it with deployment unit.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    @Override
    protected void build(final Deployment dep, final DeploymentUnit unit) {
//        final JBossMetaData jbmd = this.getAndPropagateAttachment(JBossMetaData.class, unit, dep);
//        final WebservicesMetaData wsMetaData = this.getAndPropagateAttachment(WebservicesMetaData.class, unit, dep);
//        this.getAndPropagateAttachment(WebServiceDeployment.class, unit, dep);
//
//        this.log.debug("Creating JAXRPC EJB21 endpoints meta data model");
//        for (final WebserviceDescriptionMetaData webserviceDescriptionMD : wsMetaData.getWebserviceDescriptions()) {
//            for (final PortComponentMetaData portComponentMD : webserviceDescriptionMD.getPortComponents()) {
//                final String ejbName = portComponentMD.getEjbLink();
//                this.log.debug("EJB21 name: " + ejbName);
//                final JBossEnterpriseBeanMetaData beanMetaData = jbmd.getEnterpriseBean(ejbName);
//                final String ejbClass = beanMetaData.getEjbClass();
//                this.log.debug("EJB21 class: " + ejbClass);
//
//                this.newHttpEndpoint(ejbClass, ejbName, dep);
//            }
//        }
    }
}
