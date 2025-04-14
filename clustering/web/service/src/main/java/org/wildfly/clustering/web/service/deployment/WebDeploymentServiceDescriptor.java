/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.deployment;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.service.descriptor.ServiceDescriptor;

import jakarta.servlet.ServletContext;

/**
 * Describes a service of a web deployment.
 * @author Paul Ferraro
 */
public interface WebDeploymentServiceDescriptor<T> extends ServiceDescriptor<T> {
    @SuppressWarnings("unchecked")
    WebDeploymentServiceDescriptor<UnaryOperator<String>> ROUTE_LOCATOR = WebDeploymentServiceDescriptor.of("route-locator", (Class<UnaryOperator<String>>) (Class<?>) UnaryOperator.class);
    @SuppressWarnings("unchecked")
    WebDeploymentServiceDescriptor<SessionManagerFactory<ServletContext, Map<String, Object>>> SESSION_MANAGER_FACTORY = WebDeploymentServiceDescriptor.of("session-manager-factory", (Class<SessionManagerFactory<ServletContext, Map<String, Object>>>) (Class<?>) SessionManagerFactory.class);

    default ServiceName resolve(DeploymentUnit unit) {
        return unit.getServiceName().append("distributable", this.getName());
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
