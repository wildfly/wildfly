/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Supplier;

/**
 * Encapsulates logic for registering a service dependency that supplies a value.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.service.ServiceDependency}.
 */
@Deprecated(forRemoval = true)
public interface SupplierDependency<T> extends Supplier<T>, Dependency {

}
