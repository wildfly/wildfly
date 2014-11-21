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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.picketlink.identity.federation.bindings.wildfly.idp.IDPServletExtension;
import org.picketlink.identity.federation.core.saml.v2.interfaces.SAML2Handler;
import org.picketlink.identity.federation.web.handlers.saml2.RolesGenerationHandler;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2AuthenticationHandler;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2EncryptionHandler;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2IssuerTrustHandler;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2LogOutHandler;
import org.picketlink.identity.federation.web.handlers.saml2.SAML2SignatureValidationHandler;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.config.IDPConfiguration;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityProviderService extends EntityProviderService<IdentityProviderService, IDPConfiguration> {

    private static final String SERVICE_NAME = "IDPConfigurationService";

    public IdentityProviderService(IDPConfiguration idpConfiguration) {
        super(idpConfiguration);
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(FederationExtension.SUBSYSTEM_NAME, SERVICE_NAME, alias);
    }

    @Override
    public void start(final StartContext context) throws StartException {
        super.start(context);
        setConfiguration(getConfiguration());
    }

    @Override
    public void stop(final StopContext context) {
        super.stop(context);
        setConfiguration(null);
    }

    void addTrustedDomain(final String domainName) {
        getConfiguration().addTrustDomain(domainName);
    }

    void removeTrustedDomain(final String domainName) {
        getConfiguration().removeTrustDomain(domainName);
    }

    @Override
    protected void configureWebComponents(DeploymentUnit deploymentUnit) {
        deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_SERVLET_EXTENSIONS, new IDPServletExtension(new DomainModelConfigProvider(getPicketLinkType()), getMetrics()));
    }

    @Override
    protected List<Class<? extends SAML2Handler>> getDefaultHandlers() {
        List<Class<? extends SAML2Handler>> defaultHandlers = new ArrayList<Class<? extends SAML2Handler>>();

        defaultHandlers.add(SAML2IssuerTrustHandler.class);
        defaultHandlers.add(SAML2LogOutHandler.class);
        defaultHandlers.add(SAML2AuthenticationHandler.class);
        defaultHandlers.add(RolesGenerationHandler.class);
        defaultHandlers.add(SAML2EncryptionHandler.class);
        defaultHandlers.add(SAML2SignatureValidationHandler.class);

        return defaultHandlers;

    }

    @Override
    protected void doConfigureDeployment(DeploymentUnit deploymentUnit) {
    }

    @Override
    public void setConfiguration(IDPConfiguration configuration) {
        if (configuration != null) {
            configuration.setTrust(getConfiguration().getTrust());
        }

        super.setConfiguration(configuration);
        getFederationService().getValue().setIdpConfiguration(configuration);
    }
}
