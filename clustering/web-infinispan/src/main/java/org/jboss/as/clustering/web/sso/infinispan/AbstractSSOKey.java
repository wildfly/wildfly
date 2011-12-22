/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.clustering.web.sso.infinispan;

import java.io.Serializable;

/**
 * @author Paul Ferraro
 *
 * @param <V>
 */
public abstract class AbstractSSOKey<V> implements SSOKey, Serializable {
    private static final long serialVersionUID = -4838885706919457570L;

    private String id;

    AbstractSSOKey(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !this.getClass().isInstance(object))
            return false;

        AbstractSSOKey<?> key = (AbstractSSOKey<?>) object;

        return this.id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode() ^ this.getClass().hashCode();
    }

    @Override
    public String toString() {
        return this.id;
    }
}
