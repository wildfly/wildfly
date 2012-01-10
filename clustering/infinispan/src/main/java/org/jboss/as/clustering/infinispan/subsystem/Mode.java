/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;

/**
 * @author Paul Ferraro
 */
public enum Mode {
    SYNC(true),
    ASYNC(false),
    ;
    private final boolean sync;
    private Mode(boolean sync) {
        this.sync = sync;
    }

    public static Mode forCacheMode(CacheMode mode) {
        return mode.isSynchronous() ? SYNC : ASYNC;
    }

    public CacheMode apply(CacheMode mode) {
        return this.sync ? mode.toSync() : mode.toAsync();
    }

    public boolean isSynchronous() {
        return this.sync;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
