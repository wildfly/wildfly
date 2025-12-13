/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Unifies Supplier with Serializable, for use in {@code StatelessSession} implementations
 * which themselves need to be Serializable.
 */
public interface ScopedStatelessSessionSupplier extends Supplier<AutoCloseable>, Serializable {
}
