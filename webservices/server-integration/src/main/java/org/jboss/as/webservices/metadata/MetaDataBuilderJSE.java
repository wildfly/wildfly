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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.logging.Logger;
import org.jboss.metadata.common.jboss.WebserviceDescriptionMetaData;
import org.jboss.metadata.common.jboss.WebserviceDescriptionsMetaData;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.WebModuleMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.SecurityConstraintMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionMetaData;
import org.jboss.metadata.web.spec.WebResourceCollectionsMetaData;
import org.jboss.ws.common.integration.WSConstants;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.JSEArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSESecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.JSESecurityMetaData.JSEResourceCollection;
import org.jboss.wsf.spi.metadata.j2ee.PublishLocationAdapter;

/**
 * Builds container independent meta data from WEB container meta data.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class MetaDataBuilderJSE {
    /** Logger. */
    private final Logger log = Logger.getLogger(MetaDataBuilderJSE.class);

    /**
     * Constructor.
     */
    MetaDataBuilderJSE() {
        super();
    }

    /**
     * Builds universal JSE meta data model that is AS agnostic.
     *
     * @param dep webservice deployment
     * @return universal JSE meta data model
     */
    JSEArchiveMetaData create(final Deployment dep) {
        final JBossWebMetaData jbossWebMD = WSHelper.getRequiredAttachment(dep, JBossWebMetaData.class);
        final JSEArchiveMetaData jseArchiveMD = new JSEArchiveMetaData();

        // set context root
        final String contextRoot = this.getContextRoot(dep, jbossWebMD);
        jseArchiveMD.setContextRoot(contextRoot);

        // set servlet url patterns mappings
        final Map<String, String> servletMappings = this.getServletUrlPatternsMappings(jbossWebMD);
        jseArchiveMD.setServletMappings(servletMappings);

        // set servlet class names mappings
        final Map<String, String> servletClassNamesMappings = this.getServletClassMappings(jbossWebMD);
        jseArchiveMD.setServletClassNames(servletClassNamesMappings);

        // set security domain
        final String securityDomain = jbossWebMD.getSecurityDomain();
        jseArchiveMD.setSecurityDomain(securityDomain);

        // set wsdl location resolver
        final PublishLocationAdapter resolver = new PublishLocationAdapterImpl(jbossWebMD.getWebserviceDescriptions());
        jseArchiveMD.setPublishLocationAdapter(resolver);

        // set security meta data
        final List<JSESecurityMetaData> jseSecurityMDs = this.getSecurityMetaData(jbossWebMD.getSecurityConstraints());
        jseArchiveMD.setSecurityMetaData(jseSecurityMDs);

        // set config name and file
        this.setConfigNameAndFile(jseArchiveMD, jbossWebMD);

        return jseArchiveMD;
    }

    /**
     * Sets config name and config file.
     *
     * @param jseArchiveMD universal JSE meta data model
     * @param jbossWebMD jboss web meta data
     */
    private void setConfigNameAndFile(final JSEArchiveMetaData jseArchiveMD, final JBossWebMetaData jbossWebMD) {
        final WebserviceDescriptionsMetaData wsDescriptionsMD = jbossWebMD.getWebserviceDescriptions();
        final WebserviceDescriptionMetaData wsDescriptionMD = ASHelper.getWebserviceDescriptionMetaData(wsDescriptionsMD);
        if (wsDescriptionMD != null) {
            if (wsDescriptionMD.getConfigName() != null) {
                jseArchiveMD.setConfigName(wsDescriptionMD.getConfigName());
                jseArchiveMD.setConfigFile(wsDescriptionMD.getConfigFile());

                // ensure higher priority against web.xml context parameters
                return;
            }
        }

        final List<ParamValueMetaData> contextParams = jbossWebMD.getContextParams();
        if (contextParams != null) {
            for (final ParamValueMetaData contextParam : contextParams) {
                if (WSConstants.JBOSSWS_CONFIG_NAME.equals(contextParam.getParamName())) {
                    jseArchiveMD.setConfigName(contextParam.getParamValue());
                }
                if (WSConstants.JBOSSWS_CONFIG_FILE.equals(contextParam.getParamName())) {
                    jseArchiveMD.setConfigFile(contextParam.getParamValue());
                }
            }
        }
    }

    /**
     * Builds security meta data.
     *
     * @param securityConstraintsMD security constraints meta data
     * @return universal JSE security meta data model
     */
    private List<JSESecurityMetaData> getSecurityMetaData(final List<SecurityConstraintMetaData> securityConstraintsMD) {
        final List<JSESecurityMetaData> jseSecurityMDs = new LinkedList<JSESecurityMetaData>();

        if (securityConstraintsMD != null) {
            for (final SecurityConstraintMetaData securityConstraintMD : securityConstraintsMD) {
                final JSESecurityMetaData jseSecurityMD = new JSESecurityMetaData();

                // transport guarantee
                jseSecurityMD.setTransportGuarantee(securityConstraintMD.getTransportGuarantee().name());

                // web resources
                this.setWebResources(jseSecurityMD, securityConstraintMD);

                jseSecurityMDs.add(jseSecurityMD);
            }
        }

        return jseSecurityMDs;
    }

    /**
     * Sets web resources in universal meta data model.
     *
     * @param jseSecurityMD universal JSE security meta data model
     * @param securityConstraintMD security constraint meta data
     */
    private void setWebResources(final JSESecurityMetaData jseSecurityMD, final SecurityConstraintMetaData securityConstraintMD) {
        final WebResourceCollectionsMetaData webResources = securityConstraintMD.getResourceCollections();

        for (final WebResourceCollectionMetaData webResourceMD : webResources) {
            final JSEResourceCollection jseResource = jseSecurityMD.addWebResource(webResourceMD.getName());

            for (final String webResourceUrlPatterns : webResourceMD.getUrlPatterns()) {
                jseResource.addPattern(webResourceUrlPatterns);
            }
        }
    }

    /**
     * Returns servlet name to url pattern mappings.
     *
     * @param jbossWebMD jboss web meta data
     * @return servlet name to url pattern mappings
     */
    private Map<String, String> getServletUrlPatternsMappings(final JBossWebMetaData jbossWebMD) {
        final Map<String, String> mappings = new HashMap<String, String>();
        final List<ServletMappingMetaData> servletMappings = jbossWebMD.getServletMappings();

        if (servletMappings != null) {
            for (final ServletMappingMetaData mapping : servletMappings) {
                mappings.put(mapping.getServletName(), mapping.getUrlPatterns().get(0));
            }
        }

        return mappings;
    }

    /**
     * Returns servlet name to servlet class mappings.
     *
     * @param jbossWebMD jboss web meta data
     * @return servlet name to servlet mappings
     */
    private Map<String, String> getServletClassMappings(final JBossWebMetaData jbossWebMD) {
        final Map<String, String> mappings = new HashMap<String, String>();
        final JBossServletsMetaData servlets = jbossWebMD.getServlets();

        if (servlets != null) {
            for (final ServletMetaData servlet : servlets) {
                if (servlet.getServletClass() == null || servlet.getServletClass().trim().length() == 0) {
                    // Skip JSPs
                    continue;
                }

                this.log.debug("Creating JBoss agnostic JSE meta data for POJO bean: " + servlet.getServletClass());
                mappings.put(servlet.getName(), servlet.getServletClass());
            }
        }

        return mappings;
    }

    /**
     * Returns context root associated with webservice deployment.
     *
     * If there's application.xml descriptor provided defining nested web module, then context root defined there will be
     * returned. Otherwise context root defined in jboss-web.xml will be returned.
     *
     * @param dep webservice deployment
     * @param jbossWebMD jboss web meta data
     * @return context root
     */
    private String getContextRoot(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        final JBossAppMetaData jbossAppMD = unit.getParent() == null ? null : ASHelper.getOptionalAttachment(unit.getParent(),
                WSAttachmentKeys.JBOSS_APP_METADATA_KEY);

        String contextRoot = null;

        if (jbossAppMD != null) {
            final ModuleMetaData moduleMD = jbossAppMD.getModule(dep.getSimpleName());
            if (moduleMD != null) {
                final WebModuleMetaData webModuleMD = (WebModuleMetaData) moduleMD.getValue();
                contextRoot = webModuleMD.getContextRoot();
            }
        }

        // prefer context root defined in application.xml over one defined in jboss-web.xml
        return contextRoot != null ? contextRoot : jbossWebMD.getContextRoot();
    }
}
