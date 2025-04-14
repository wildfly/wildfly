/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.sso.elytron;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.web.container.SingleSignOnManagerConfiguration;
import org.wildfly.clustering.web.container.SingleSignOnManagerServiceInstallerProvider;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnManager;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Singleton reference to a non-distributable {@link SingleSignOnManagerServiceInstallerProvider}.
 * @author Paul Ferraro
 */
public enum NonDistributableSingleSignOnManagementProvider implements SingleSignOnManagerServiceInstallerProvider {
    INSTANCE;

    @Override
    public ResourceServiceInstaller getServiceInstaller(SingleSignOnManagerConfiguration configuration) {
        Supplier<SingleSignOnManager> factory = new Supplier<>() {
            @Override
            public SingleSignOnManager get() {
                return new DefaultSingleSignOnManager(new ConcurrentHashMap<>(), configuration.getIdentifierGenerator());
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(ServiceNameFactory.resolveServiceName(SingleSignOnManagerServiceInstallerProvider.SINGLE_SIGN_ON_MANAGER, configuration.getSecurityDomainName()))
                .build();
    }
}
