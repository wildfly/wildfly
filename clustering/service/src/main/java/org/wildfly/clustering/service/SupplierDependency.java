/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Supplier;

/**
 * Encapsulates logic for registering a service dependency that supplies a value.
 * @author Paul Ferraro
 */
public interface SupplierDependency<T> extends Supplier<T>, Dependency {

}
