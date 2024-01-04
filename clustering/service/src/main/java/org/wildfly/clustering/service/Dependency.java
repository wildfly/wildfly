/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceBuilder;

/**
 * Encapsulates logic for registering a service dependency.
 * @author Paul Ferraro
 */
public interface Dependency {
    <T> ServiceBuilder<T> register(ServiceBuilder<T> builder);
}
