/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.management.security.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_ACCESS_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.BY_SEARCH_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CACHE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.EVICTION_TIME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.CACHE_FAILURES;
import static org.jboss.as.domain.management.ModelDescriptionConstants.MAX_CACHE_SIZE;

import org.jboss.dmr.ModelNode;

/**
 * A builder for defining a cache definition.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class CacheBuilder<T extends ParentBuilder<?>> extends Builder<T> {

    private final T parent;
    private final ModelNode parentAddress;

    private boolean built = false;

    private By by = By.SEARCH_TIME;
    private int evictionTime = -1;
    private boolean cacheFailures = false;
    private int maxCacheSize = -1;

    CacheBuilder(T parent, ModelNode parentAddress) {
        this.parent = parent;
        this.parentAddress = parentAddress;
    }

    public CacheBuilder<T> setBy(final By by) {
        assertNotBuilt();
        if (by == null) {
            throw new IllegalArgumentException("By can not be null.");
        }
        this.by = by;

        return this;
    }

    public CacheBuilder<T> setEvictionTime(final int evictionTime) {
        assertNotBuilt();
        this.evictionTime = evictionTime;

        return this;
    }

    public CacheBuilder<T> setCacheFailures(final boolean cacheFailures) {
        assertNotBuilt();
        this.cacheFailures = cacheFailures;

        return this;
    }

    public CacheBuilder<T> setMaxCacheSize(final int maxCacheSize) {
        assertNotBuilt();
        this.maxCacheSize = maxCacheSize;

        return this;
    }

    public T build() {
        assertNotBuilt();
        built = true;

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(ADDRESS).set(parentAddress.add(CACHE, by == By.ACCESS_TIME ? BY_ACCESS_TIME : BY_SEARCH_TIME));

        if (evictionTime > -1) {
            add.get(EVICTION_TIME).set(evictionTime);
        }
        if (cacheFailures) {
            add.get(CACHE_FAILURES).set(true);
        }
        if (maxCacheSize > -1) {
            add.get(MAX_CACHE_SIZE).set(maxCacheSize);
        }

        parent.addStep(add);

        return parent;
    }

    @Override
    boolean isBuilt() {
        return built;
    }

    private void assertNotBuilt() {
        parent.assertNotBuilt();
        if (built) {
            throw new IllegalStateException("Already built.");
        }
    }

    public enum By {
        ACCESS_TIME, SEARCH_TIME;
    }
}
