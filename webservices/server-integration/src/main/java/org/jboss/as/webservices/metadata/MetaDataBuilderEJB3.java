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

import java.util.LinkedList;
import java.util.List;

import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;

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
        final WebServiceDeployment ejb3Deployment = WSHelper.getRequiredAttachment(dep, WebServiceDeployment.class);
        final List<EJBMetaData> wsEjbsMD = new LinkedList<EJBMetaData>();

        for (final WebServiceDeclaration jbossEjbMD : ejb3Deployment.getServiceEndpoints()) {
            this.buildEnterpriseBeanMetaData(wsEjbsMD, jbossEjbMD);
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
    private void buildEnterpriseBeanMetaData(final List<EJBMetaData> wsEjbsMD, final WebServiceDeclaration jbossEjbMD) {
        final EJBMetaData wsEjbMD = this.newEjbMetaData(jbossEjbMD);

        if (wsEjbMD != null) {
            // set EJB name and class
            wsEjbMD.setEjbName(jbossEjbMD.getComponentName());
            wsEjbMD.setEjbClass(jbossEjbMD.getComponentClassName());

            /*
             * TODO: implement final PortComponentSpec portComponentAnnotation =
             * jbossEjbMD.getAnnotation(PortComponentSpec.class); if
             * (portComponentAnnotation != null) { // set port component meta
             * data wsEjbMD.setPortComponentName(portComponentAnnotation.
             * portComponentName());
             * wsEjbMD.setPortComponentURI(portComponentAnnotation
             * .portComponentURI());
             *
             * // set security meta data final EJBSecurityMetaData
             * wsEjbSecurityMD = new EJBSecurityMetaData();
             * wsEjbSecurityMD.setAuthMethod
             * (portComponentAnnotation.authMethod());
             * wsEjbSecurityMD.setTransportGuarantee
             * (portComponentAnnotation.transportGuarantee());
             * wsEjbSecurityMD.setSecureWSDLAccess
             * (portComponentAnnotation.secureWSDLAccess());
             * wsEjbMD.setSecurityMetaData(wsEjbSecurityMD); }
             */

            wsEjbsMD.add(wsEjbMD);
        }

    }

    /**
     * Creates new JBoss agnostic EJB bean meta data model.
     *
     * @param jbossEjbMD
     *            jboss EJB meta data
     * @return webservices EJB meta data
     */
    private EJBMetaData newEjbMetaData(final WebServiceDeclaration jbossEjbMD) {
        return new SLSBMetaData();
        /*
         * TODO: implement final MessageDriven mdbAnnotation =
         * jbossEjbMD.getAnnotation(MessageDriven.class);
         *
         * if (mdbAnnotation == null) { this.log.debug(
         * "Creating JBoss agnostic EJB3 meta data for session bean: " +
         * jbossEjbMD.getComponentClassName()); return new SLSBMetaData(); }
         * else { this.log.debug(
         * "Creating JBoss agnostic EJB3 meta data for message driven bean: " +
         * jbossEjbMD.getComponentClassName()); final MDBMetaData mdbMD = new
         * MDBMetaData();
         *
         * final String destinationName =
         * this.getActivationProperty("destination",
         * mdbAnnotation.activationConfig());
         * mdbMD.setDestinationJndiName(destinationName);
         *
         * return mdbMD; }
         */
    }

    /**
     * Returns activation config property value or null if not found.
     *
     * @param name
     *            activation property name
     * @param activationConfigProperties
     *            activation config properties
     * @return activation config property value
     */
    /*
     * private String getActivationProperty(final String name, final
     * ActivationConfigProperty[] activationConfigProperties) { if
     * (activationConfigProperties != null) { for (final
     * ActivationConfigProperty activationConfigProperty :
     * activationConfigProperties) { if
     * (activationConfigProperty.propertyName().equals(name)) { return
     * activationConfigProperty.propertyValue(); } } }
     *
     * return null; }
     */
}
