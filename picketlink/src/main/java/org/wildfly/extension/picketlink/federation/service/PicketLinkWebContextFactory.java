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

import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.web.ext.WebContextFactory;
import org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve;
import org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator;
import org.wildfly.extension.picketlink.federation.metrics.PicketLinkSubsystemMetrics;

/**
 * <p> This {@link org.jboss.as.web.ext.WebContextFactory} subclass is responsible do finish the configuration of a deployment/application as a PicketLink deployment.
 * </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkWebContextFactory implements WebContextFactory {

    private final DomainModelConfigProvider configProvider;
    private final PicketLinkSubsystemMetrics auditHelper;

    public PicketLinkWebContextFactory(DomainModelConfigProvider configProvider, PicketLinkSubsystemMetrics metrics) {
        this.configProvider = configProvider;
        this.auditHelper = metrics;
    }

    @Override
    public StandardContext createContext(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        return new StandardContext();
    }

    @Override
    public void postProcessContext(DeploymentUnit deploymentUnit, StandardContext webContext) {
        if (this.configProvider.isIdentityProviderConfiguration()) {
            addIdentityProviderValves(webContext);
        } else {
            addServiceProviderValves(webContext);
        }
    }

    private void addServiceProviderValves(StandardContext webContext) {
        ServiceProviderAuthenticator valve = new ServiceProviderAuthenticator();

        valve.setConfigProvider(this.configProvider);
        valve.setAuditHelper(this.auditHelper);

        webContext.addValve(valve);
    }

    private void addIdentityProviderValves(StandardContext webContext) {
        IDPWebBrowserSSOValve valve = new IDPWebBrowserSSOValve();

        valve.setConfigProvider(this.configProvider);
        valve.setAuditHelper(this.auditHelper);

        webContext.addValve(valve);
    }
}
