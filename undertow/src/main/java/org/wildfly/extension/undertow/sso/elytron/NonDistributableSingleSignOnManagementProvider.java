/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.sso.elytron;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementProvider;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnManager;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Singleton reference to a non-distributable {@link SecurityDomainSingleSignOnManagementProvider}.
 * @author Paul Ferraro
 */
public enum NonDistributableSingleSignOnManagementProvider implements SecurityDomainSingleSignOnManagementProvider {
    INSTANCE;

    @Override
    public ResourceServiceInstaller getServiceInstaller(OperationContext context, ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration) {
        Supplier<SingleSignOnManager> factory = new Supplier<>() {
            @Override
            public SingleSignOnManager get() {
                return new DefaultSingleSignOnManager(new ConcurrentHashMap<>(), configuration.getIdentifierGenerator());
            }
        };
        return ServiceInstaller.builder(factory).provides(name).build();
    }
}
