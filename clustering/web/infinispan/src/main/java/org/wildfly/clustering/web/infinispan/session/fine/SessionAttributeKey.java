/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session.fine;

import org.wildfly.clustering.infinispan.spi.distribution.Key;

/**
 * Cache key for session attributes.
 * @author Paul Ferraro
 */
public class SessionAttributeKey extends Key<String> {

    private final String attribute;

    public SessionAttributeKey(String id, String attribute) {
        super(id);
        this.attribute = attribute;
    }

    public String getAttribute() {
        return this.attribute;
    }

    @Override
    public int hashCode() {
        return (31 * super.hashCode()) + this.attribute.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object) && (object instanceof SessionAttributeKey) && this.attribute.equals(((SessionAttributeKey) object).attribute);
    }

    @Override
    public String toString() {
        return String.format("%s->%s", this.getValue(), this.attribute);
    }
}