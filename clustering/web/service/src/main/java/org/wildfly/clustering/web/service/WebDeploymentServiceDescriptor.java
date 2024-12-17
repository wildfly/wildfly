/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service;

import java.net.URI;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.user.UserManagerFactory;
import org.wildfly.security.cache.CachedIdentity;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

import jakarta.servlet.ServletContext;

/**
 * @author Paul Ferraro
 */
public interface WebDeploymentServiceDescriptor {
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<UnaryOperator<String>> ROUTE_LOCATOR = UnaryServiceDescriptor.of("org.wildfly.clustering.web.route-locator", (Class<UnaryOperator<String>>) (Class<?>) UnaryOperator.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<SessionManagerFactory<ServletContext, Map<String, Object>>> SESSION_MANAGER_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.web.session-manager-factory", (Class<SessionManagerFactory<ServletContext, Map<String, Object>>>) (Class<?>) SessionManagerFactory.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>> USER_MANAGER_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.web.user-manager-factory", (Class<UserManagerFactory<CachedIdentity, String, Map.Entry<String, URI>>>) (Class<?>) UserManagerFactory.class);
}
