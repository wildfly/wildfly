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

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.io.IOException;
import java.lang.reflect.Method;

import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for proxies serialized using the writeReplace()/readResolve() pattern.
 * @author Paul Ferraro
 */
public class ProxyMarshaller<T> extends FunctionalScalarMarshaller<T, Object> {

    public ProxyMarshaller(Class<? extends T> targetClass) {
        super(targetClass, Scalar.ANY, new ExceptionFunction<T, Object, IOException>() {
            @Override
            public Object apply(T object) throws IOException {
                Method method = Reflect.findMethod(object.getClass(), "writeReplace");
                return Reflect.invoke(object, method);
            }
        }, new ExceptionFunction<Object, T, IOException>() {
            @Override
            public T apply(Object proxy) throws IOException {
                Method method = Reflect.findMethod(proxy.getClass(), "readResolve");
                return Reflect.invoke(proxy, method, targetClass);
            }
        });
    }
}
