/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public class SingleSignOnSessionFactoryServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public SingleSignOnSessionFactoryServiceNameProvider(String securityDomainName) {
        this.name = ApplicationSecurityDomainDefinition.APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.fromBaseCapability(securityDomainName).getCapabilityServiceName().append("sso", "factory");
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }
}
