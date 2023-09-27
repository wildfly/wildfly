/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementProvider;
import org.wildfly.clustering.web.service.sso.LegacySSOManagementProviderFactory;

/**
 * {@link org.wildfly.extension.undertow.session.SessionManagementProviderFactory} for Undertow using either the {@link org.wildfly.clustering.web.sso.DistributableSSOManagementProvider} for the given security domain, the default provider, or a legacy provider.
 * @author Paul Ferraro
 */
@MetaInfServices(SecurityDomainSingleSignOnManagementProvider.class)
public class UndertowSingleSignOnManagementProvider implements SecurityDomainSingleSignOnManagementProvider {

    private final LegacySSOManagementProviderFactory legacyProviderFactory;

    public UndertowSingleSignOnManagementProvider() {
        Iterator<LegacySSOManagementProviderFactory> factories = ServiceLoader.load(LegacySSOManagementProviderFactory.class, LegacySSOManagementProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacySSOManagementProviderFactory.class.getName());
        }
        this.legacyProviderFactory = factories.next();
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration) {
        return new DistributableSingleSignOnManagerServiceConfigurator(name, configuration, this.legacyProviderFactory);
    }
}
