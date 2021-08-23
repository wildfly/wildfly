/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
