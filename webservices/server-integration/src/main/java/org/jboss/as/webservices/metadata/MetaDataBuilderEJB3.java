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
package org.jboss.as.webservices.metadata;

import static org.jboss.as.webservices.util.ASHelper.getContextRoot;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class MetaDataBuilderEJB3 extends AbstractMetaDataBuilderEJB {
    /**
     * Constructor.
     */
    MetaDataBuilderEJB3() {
        super();
    }

    /**
     * @see AbstractMetaDataBuilderEJB#buildEnterpriseBeansMetaData(Deployment,
     *      EJBArchiveMetaData)
     *
     * @param dep
     *            webservice deployment
     * @param ejbArchiveMD
     *            EJB archive meta data
     */
    @Override
    protected void buildEnterpriseBeansMetaData(final Deployment dep, final EJBArchiveMetaData ejbArchiveMD) {
        if (!WSHelper.isJaxwsJseDeployment(dep)) { // [AS7-1605] support
            final JBossWebMetaData jbossWebMD = WSHelper.getOptionalAttachment(dep, JBossWebMetaData.class);
            final String contextRoot = getContextRoot(dep, jbossWebMD);
            if (contextRoot != null) {
                final JSEArchiveMetaData jseArchiveMD = new JSEArchiveMetaData();
                jseArchiveMD.setContextRoot(contextRoot);
                dep.addAttachment(JSEArchiveMetaData.class, jseArchiveMD);
            }
        }

        final JAXWSDeployment ejb3Deployment = WSHelper.getRequiredAttachment(dep, JAXWSDeployment.class);
        final List<EJBMetaData> wsEjbsMD = new LinkedList<EJBMetaData>();
        final JBossWebservicesMetaData jbossWebservicesMD = WSHelper.getOptionalAttachment(dep, JBossWebservicesMetaData.class);

        for (final EJBEndpoint jbossEjbMD : ejb3Deployment.getEjbEndpoints()) {
            this.buildEnterpriseBeanMetaData(wsEjbsMD, jbossEjbMD, jbossWebservicesMD);
        }

        ejbArchiveMD.setEnterpriseBeans(wsEjbsMD);
    }

    /**
     * Builds JBoss agnostic EJB meta data.
     *
     * @param wsEjbsMD
     *            jboss agnostic EJBs meta data
     * @param jbossEjbMD
     *            jboss specific EJB meta data
     */
    private void buildEnterpriseBeanMetaData(final List<EJBMetaData> wsEjbsMD, final EJBEndpoint jbossEjbMD, final JBossWebservicesMetaData jbossWebservicesMD) {
        final EJBMetaData wsEjbMD = new SLSBMetaData();

        if (wsEjbMD != null) {
            // set EJB name and class
            wsEjbMD.setEjbName(jbossEjbMD.getName());
            wsEjbMD.setEjbClass(jbossEjbMD.getClassName());

            final PortComponentMetaData portComponentMD = getPortComponent(jbossEjbMD.getName(), jbossWebservicesMD);
            if (portComponentMD != null) {
               // set port component meta data
               wsEjbMD.setPortComponentName(portComponentMD.getPortComponentName());
               wsEjbMD.setPortComponentURI(portComponentMD.getPortComponentURI());

               // set security meta data
               final EJBSecurityMetaData smd = new EJBSecurityMetaData();
               smd.setAuthMethod(portComponentMD.getAuthMethod());
               smd.setTransportGuarantee(portComponentMD.getTransportGuarantee());
               smd.setSecureWSDLAccess(portComponentMD.getSecureWSDLAccess());
               wsEjbMD.setSecurityMetaData(smd);
            }

            wsEjbsMD.add(wsEjbMD);
        }

    }

}
