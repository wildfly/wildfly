/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reference to some context.
 * @author Paul Ferraro
 */
public interface ContextReference<C> extends Supplier<C>, Consumer<C> {
}
