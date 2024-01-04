/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.sso.elytron;

import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.web.container.SecurityDomainSingleSignOnManagementConfiguration;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnManager;

/**
 * Configures a srevice providing a non-distributable {@link SingleSignOnManager}.
 * @author Paul Ferraro
 */
public class DefaultSingleSignOnManagerServiceConfigurator extends SimpleCapabilityServiceConfigurator<SingleSignOnManager> {

    public DefaultSingleSignOnManagerServiceConfigurator(ServiceName name, SecurityDomainSingleSignOnManagementConfiguration configuration) {
        super(name, new DefaultSingleSignOnManager(new ConcurrentHashMap<>(), configuration.getIdentifierGenerator()));
    }
}
