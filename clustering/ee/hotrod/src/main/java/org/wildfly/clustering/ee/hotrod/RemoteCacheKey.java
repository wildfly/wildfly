/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.util.Objects;

import org.wildfly.clustering.ee.Key;

/**
 * Base type for remote cache keys.
 * @author Paul Ferraro
 */
public class RemoteCacheKey<I> implements Key<I> {

    private I id;

    public RemoteCacheKey(I id) {
        this.id = id;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass(), this.id);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        return this.getClass().equals(object.getClass()) && this.id.equals(((RemoteCacheKey<?>) object).id);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.getClass().getSimpleName(), this.id);
    }
}
