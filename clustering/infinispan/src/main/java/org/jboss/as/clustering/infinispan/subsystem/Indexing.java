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

/**
 * Used to configure indexing of entries in the cache for searching.
 *
 * @author Paul Ferraro
 */
public enum Indexing {
    NONE(false, false),
    LOCAL(true, true),
    ALL(true, false),
    ;
    private final boolean enabled;
    private final boolean localOnly;

    private Indexing(boolean enabled, boolean localOnly) {
        this.enabled = enabled;
        this.localOnly = localOnly;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isLocalOnly() {
        return this.localOnly;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
