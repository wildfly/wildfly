/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

/**
 * Implemented by keys that should be logical grouped by a common identifier.
 * Keys with the same group identifier will be stored within the same segment.
 * This is analogous to Infinispan's {@link org.infinispan.distribution.group.Group} logic, but avoids a potentially unnecessary String conversion.
 * @author Paul Ferraro
 */
public interface KeyGroup<I> {
    /**
     * The identifier of this group of keys.
     * @return an group identifier
     */
    I getId();
}
