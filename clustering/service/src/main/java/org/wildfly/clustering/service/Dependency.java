/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;

/**
 * Encapsulates logic for registering a service dependency.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.Dependency}.
 */
@Deprecated(forRemoval = true)
public interface Dependency {
    <T> ServiceBuilder<T> register(ServiceBuilder<T> builder);
}
