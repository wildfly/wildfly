/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import java.io.IOException;
import java.io.Serializable;

import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.SimpleMarshalledValue;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;

import static org.jboss.as.clustering.web.impl.ClusteringWebMessages.MESSAGES;

/**
 * Session attribute marshaller that marshals attribute values using a {@link SimpleMarshalledValue}.
 *
 * @author Paul Ferraro
 */
public class SessionAttributeMarshallerImpl implements SessionAttributeMarshaller {
    private final MarshallingContext context;

    public SessionAttributeMarshallerImpl(MarshallingContext context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.SessionAttributeMarshaller#marshal(Object)
     */
    @Override
    public Object marshal(Object value) throws IOException {
        if ((value == null) || isTypeExcluded(value.getClass())) {
            return value;
        }
        if (!(value instanceof Serializable)) {
            throw MESSAGES.interfaceNotImplemented(value, Serializable.class.getName());
        }
        return new SimpleMarshalledValue<Object>(value, this.context);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.SessionAttributeMarshaller#unmarshal(Object)
     */
    @Override
    public Object unmarshal(Object object) throws IOException, ClassNotFoundException {
        if ((object == null) || !(object instanceof SimpleMarshalledValue)) {
            return object;
        }
        SimpleMarshalledValue<?> value = (SimpleMarshalledValue<?>) object;
        return value.get(this.context);
    }

    public static boolean isTypeExcluded(Class<?> type) {
        return type.equals(String.class) || type.isPrimitive() || type.equals(Void.class) || type.equals(Boolean.class)
                || type.equals(Character.class) || type.equals(Byte.class) || type.equals(Short.class)
                || type.equals(Integer.class) || type.equals(Long.class) || type.equals(Float.class)
                || type.equals(Double.class) || (type.isArray() && isTypeExcluded(type.getComponentType()));
    }
}
