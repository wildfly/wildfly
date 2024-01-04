/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
