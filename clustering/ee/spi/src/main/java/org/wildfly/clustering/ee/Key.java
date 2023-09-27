/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

/**
 * A cache key for a given identifier
 * @author Paul Ferraro
 */
public interface Key<I> {
    I getId();
}
