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
package org.jboss.as.webservices.metadata;

import java.util.List;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.PublishLocationAdapter;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossPortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractMetaDataBuilderEJB {

    /**
     * Builds universal EJB meta data model that is AS agnostic.
     *
     * @param dep
     *            webservice deployment
     * @return universal EJB meta data model
     */
    final EJBArchiveMetaData create(final Deployment dep) {
        if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
            WSLogger.ROOT_LOGGER.tracef("Building JBoss agnostic meta data for EJB webservice deployment: %s", dep.getSimpleName());
        }
        final EJBArchiveMetaData.Builder ejbArchiveMDBuilder = new EJBArchiveMetaData.Builder();

        this.buildEnterpriseBeansMetaData(dep, ejbArchiveMDBuilder);
        this.buildWebservicesMetaData(dep, ejbArchiveMDBuilder);

        return ejbArchiveMDBuilder.build();
    }

    /**
     * Template method for build enterprise beans meta data.
     *
     * @param dep
     *            webservice deployment
     * @param ejbMetaData
     *            universal EJB meta data model
     */
    protected abstract void buildEnterpriseBeansMetaData(Deployment dep, EJBArchiveMetaData.Builder ejbMetaDataBuilder);

    /**
     * Builds webservices meta data. This methods sets:
     * <ul>
     *   <li>context root</li>
     *   <li>wsdl location resolver</li>
     *   <li>config name</li>
     *   <li>config file</li>
     * </ul>
     *
     * @param dep webservice deployment
     * @param ejbArchiveMD universal EJB meta data model
     */
    private void buildWebservicesMetaData(final Deployment dep, final EJBArchiveMetaData.Builder ejbArchiveMDBuilder) {
       final JBossWebservicesMetaData webservicesMD = WSHelper.getOptionalAttachment(dep, JBossWebservicesMetaData.class);

       if (webservicesMD == null) return;

       // set context root
       final String contextRoot = webservicesMD.getContextRoot();
       ejbArchiveMDBuilder.setWebServiceContextRoot(contextRoot);
        WSLogger.ROOT_LOGGER.tracef("Setting context root: %s", contextRoot);

       // set config name
       final String configName = webservicesMD.getConfigName();
       ejbArchiveMDBuilder.setConfigName(configName);
        WSLogger.ROOT_LOGGER.tracef("Setting config name: %s", configName);

       // set config file
       final String configFile = webservicesMD.getConfigFile();
       ejbArchiveMDBuilder.setConfigFile(configFile);
        WSLogger.ROOT_LOGGER.tracef("Setting config file: %s", configFile);

       // set wsdl location resolver
       final JBossWebserviceDescriptionMetaData[] wsDescriptionsMD = webservicesMD.getWebserviceDescriptions();
       final PublishLocationAdapter resolver = new PublishLocationAdapterImpl(wsDescriptionsMD);
       ejbArchiveMDBuilder.setPublishLocationAdapter(resolver);
    }

    protected JBossPortComponentMetaData getPortComponent(final String ejbName, final JBossWebservicesMetaData jbossWebservicesMD) {
        if (jbossWebservicesMD == null) return null;

        for (final JBossPortComponentMetaData jbossPortComponentMD : jbossWebservicesMD.getPortComponents()) {
            if (ejbName.equals(jbossPortComponentMD.getEjbName())) return jbossPortComponentMD;
        }

        return null;
    }

    /**
     * Builds JBoss agnostic EJB meta data.
     *
     * @param wsEjbsMD
     *            jboss agnostic EJBs meta data
     */
    protected void buildEnterpriseBeanMetaData(final List<EJBMetaData> wsEjbsMD, final EJBEndpoint ejbEndpoint, final JBossWebservicesMetaData jbossWebservicesMD) {
        final SLSBMetaData.Builder wsEjbMDBuilder = new SLSBMetaData.Builder();

        // set EJB name and class
        wsEjbMDBuilder.setEjbName(ejbEndpoint.getName());
        wsEjbMDBuilder.setEjbClass(ejbEndpoint.getClassName());

        final JBossPortComponentMetaData portComponentMD = getPortComponent(ejbEndpoint.getName(), jbossWebservicesMD);
        if (portComponentMD != null) {
            // set port component meta data
            wsEjbMDBuilder.setPortComponentName(portComponentMD.getPortComponentName());
            wsEjbMDBuilder.setPortComponentURI(portComponentMD.getPortComponentURI());
        }
        // set security meta data
        // auth method
        final String authMethod = getAuthMethod(ejbEndpoint, portComponentMD);
        // transport guarantee
        final String transportGuarantee = getTransportGuarantee(ejbEndpoint, portComponentMD);
        // secure wsdl access
        final boolean secureWsdlAccess = isSecureWsdlAccess(ejbEndpoint, portComponentMD);

        final String realmName = getRealmName(ejbEndpoint, portComponentMD);
        // propagate
        wsEjbMDBuilder.setSecurityMetaData(new EJBSecurityMetaData(authMethod, realmName, transportGuarantee, secureWsdlAccess));

        wsEjbsMD.add(wsEjbMDBuilder.build());
    }

    private static String getAuthMethod(final EJBEndpoint ejbEndpoint, final JBossPortComponentMetaData portComponentMD) {
        if (ejbEndpoint.getAuthMethod() != null) return ejbEndpoint.getAuthMethod();
        return portComponentMD != null ? portComponentMD.getAuthMethod() : null;
    }

    private static String getTransportGuarantee(final EJBEndpoint ejbEndpoint, final JBossPortComponentMetaData portComponentMD) {
        if (portComponentMD != null && portComponentMD.getTransportGuarantee() != null) return portComponentMD.getTransportGuarantee();
        return ejbEndpoint != null ? ejbEndpoint.getTransportGuarantee() : null;
    }

    private static boolean isSecureWsdlAccess(final EJBEndpoint ejbEndpoint, final JBossPortComponentMetaData portComponentMD) {
        if (ejbEndpoint.isSecureWsdlAccess()) return true;
        return (portComponentMD != null && portComponentMD.getSecureWSDLAccess() != null) ? portComponentMD.getSecureWSDLAccess() : false;
    }

    private static String getRealmName(final EJBEndpoint ejbEndpoint, final JBossPortComponentMetaData portComponentMD) {
       if (ejbEndpoint.getRealmName() != null) return ejbEndpoint.getRealmName();
       return portComponentMD != null ? portComponentMD.getRealmName() : null;
    }

}
