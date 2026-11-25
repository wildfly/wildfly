/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.session.SessionAffinityProvider;
import org.wildfly.service.descriptor.ServiceDescriptor;

/**
 * Describes deployment services.
 * TODO Relocate to SPI module.
 * @author Paul Ferraro
 */
public interface WebDeploymentServiceDescriptor<T> extends ServiceDescriptor<T> {
    WebDeploymentServiceDescriptor<Deployment> DEPLOYMENT = of("deployment", Deployment.class);
    WebDeploymentServiceDescriptor<DeploymentInfo> DEPLOYMENT_INFO = of("deployment-info", DeploymentInfo.class);
    WebDeploymentServiceDescriptor<SessionManagerFactory> SESSION_MANAGER_FACTORY = of("session-manager-factory", SessionManagerFactory.class);
    WebDeploymentServiceDescriptor<SessionAffinityProvider> SESSION_AFFINITY_PROVIDER = WebDeploymentServiceDescriptor.of("session-affinity-provider", SessionAffinityProvider.class);

    default ServiceName resolve(DeploymentUnit unit) {
        return unit.getServiceName().append("undertow", this.getName());
    }

    @Override
    default <U extends T> WebDeploymentServiceDescriptor<U> asType(Class<U> type) {
        return new WebDeploymentServiceDescriptor<>() {
            @Override
            public String getName() {
                return WebDeploymentServiceDescriptor.this.getName();
            }

            @Override
            public Class<U> getType() {
                return type;
            }

            @Override
            public ServiceName resolve(DeploymentUnit unit) {
                return WebDeploymentServiceDescriptor.this.resolve(unit);
            }
        };
    }

    static <T> WebDeploymentServiceDescriptor<T> of(String name, Class<T> type) {
        return new WebDeploymentServiceDescriptor<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<T> getType() {
                return type;
            }
        };
    }
}
