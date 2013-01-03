/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SimpleAttachmentKey<T> extends AttachmentKey<T> {
    private final Class<T> valueClass;

    SimpleAttachmentKey(final Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    public T cast(final Object value) {
        return valueClass.cast(value);
    }

    @Override
    public String toString() {
        if (valueClass != null) {
            StringBuilder sb = new StringBuilder(getClass().getName());
            sb.append("<");
            sb.append(valueClass.getName());
            sb.append(">");
            return sb.toString();
        }
        return super.toString();
    }
}
