/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.function.UnaryOperator;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A decorator marshaller that writes the decorated object while holding its monitor lock.
 * e.g. to enable iteration over a decorated collection without the risk of a ConcurrentModificationException.
 * @author Paul Ferraro
 */
public class SynchronizedDecoratorMarshaller<T> extends DecoratorMarshaller<T> {

    /**
     * Constructs a decorator marshaller.
     * @param decoratedClass the generalized type of the decorated object
     * @param decorator the decoration function
     * @param sample a sample object used to determine the type of the decorated object
     */
    public SynchronizedDecoratorMarshaller(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        super(decoratedClass, decorator, sample);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        T decorated = WildFlySecurityManager.doUnchecked(value, this);
        if (decorated != null) {
            synchronized (value) {
                writer.writeAny(DECORATED_INDEX, decorated);
            }
        }
    }
}
