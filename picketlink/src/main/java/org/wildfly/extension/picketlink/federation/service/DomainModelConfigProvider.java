/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.federation.service;

import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.config.federation.IDPType;
import org.picketlink.config.federation.PicketLinkType;
import org.picketlink.config.federation.ProviderType;
import org.picketlink.config.federation.SPType;
import org.picketlink.identity.federation.web.config.AbstractSAMLConfigurationProvider;
import org.picketlink.identity.federation.web.config.IDPMetadataConfigurationProvider;
import org.picketlink.identity.federation.web.config.SPPostMetadataConfigurationProvider;
import org.picketlink.identity.federation.web.config.SPRedirectMetadataConfigurationProvider;
import org.wildfly.extension.picketlink.federation.config.IDPConfiguration;
import org.wildfly.extension.picketlink.federation.config.SPConfiguration;
import org.wildfly.extension.picketlink.logging.PicketLinkLogger;

import java.io.InputStream;

/**
 * <p> {@link org.picketlink.identity.federation.web.util.SAMLConfigurationProvider} to be used to with identity providers and service providers
 * deployments considering the configurations defined in the subsystem. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class DomainModelConfigProvider extends AbstractSAMLConfigurationProvider {

    private final PicketLinkType configuration;

    public DomainModelConfigProvider(PicketLinkType picketLinkType) {
        this.configuration = picketLinkType;
    }

    @Override
    public IDPType getIDPConfiguration() {
        ProviderType providerType = getPicketLinkConfiguration().getIdpOrSP();

        if (providerType instanceof IDPConfiguration) {
            IDPConfiguration configuration = (IDPConfiguration) providerType;

            if (configuration.isSupportMetadata()) {
                try {
                    IDPType metadataConfig = new IDPMetadataConfigurationProvider().getIDPConfiguration();

                    metadataConfig.importFrom(configuration);

                    providerType = metadataConfig;
                } catch (ProcessingException e) {
                    throw PicketLinkLogger.ROOT_LOGGER.federationSAMLMetadataConfigError(configuration.getAlias(), e);
                }
            }

            if (configParsedIDPType != null) {
                configuration.importFrom(configParsedIDPType);
            }

            return (IDPType) providerType;
        }

        return null;
    }

    @Override
    public SPType getSPConfiguration() {
        ProviderType providerType = this.getPicketLinkConfiguration().getIdpOrSP();

        if (providerType instanceof SPConfiguration) {
            SPConfiguration configuration = (SPConfiguration) providerType;

            if (configuration.isSupportMetadata()) {
                try {
                    SPType metadataConfig;

                    if (configuration.isPostBinding()) {
                        metadataConfig = new SPPostMetadataConfigurationProvider().getSPConfiguration();
                    } else {
                        metadataConfig = new SPRedirectMetadataConfigurationProvider().getSPConfiguration();
                    }

                    metadataConfig.importFrom(configuration);

                    providerType = metadataConfig;
                } catch (ProcessingException e) {
                    throw PicketLinkLogger.ROOT_LOGGER.federationSAMLMetadataConfigError(configuration.getAlias(), e);
                }
            }

            if (configParsedSPType != null) {
                configuration.importFrom(configParsedSPType);
            }

            return (SPType) providerType;
        }

        return null;
    }

    @Override
    public PicketLinkType getPicketLinkConfiguration() {
        if (this.configParsedPicketLinkType != null) {
            if (this.configParsedPicketLinkType.getHandlers() != null) {
                this.configuration.setHandlers(this.configParsedPicketLinkType.getHandlers());
            }
            if (this.configParsedPicketLinkType.getStsType() != null) {
                this.configuration.setStsType(this.configParsedPicketLinkType.getStsType());
            }
        }

        return this.configuration;
    }

    @Override
    public void setConsolidatedConfigFile(InputStream is) throws ParsingException {
        try {
            super.setConsolidatedConfigFile(is);
        } catch (Exception e) {
            logger.trace("Configurations defined in picketlink.xml will be ignored.");
        }
    }

    boolean isIdentityProviderConfiguration() {
        return IDPConfiguration.class.isInstance(this.configuration.getIdpOrSP());
    }
}