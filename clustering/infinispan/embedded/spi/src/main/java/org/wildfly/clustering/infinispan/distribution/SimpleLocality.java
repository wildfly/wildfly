/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

/**
 * Simple {@link Locality} implementation that uses a static value.
 * @author Paul Ferraro
 */
public class SimpleLocality implements Locality {

    private final boolean local;

    public SimpleLocality(boolean local) {
        this.local = local;
    }

    @Override
    public boolean isLocal(Object key) {
        return this.local;
    }
}
