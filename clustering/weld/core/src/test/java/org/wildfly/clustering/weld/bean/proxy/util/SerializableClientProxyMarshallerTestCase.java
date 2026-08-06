/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.proxy.util;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jboss.weld.bean.StringBeanIdentifier;
import org.jboss.weld.bean.proxy.util.SerializableClientProxy;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * @author Paul Ferraro
 */
public class SerializableClientProxyMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        Tester<SerializableClientProxy> tester = factory.createTester(SerializableClientProxyMarshallerTestCase::assertEquals);
        tester.accept(new SerializableClientProxy(new StringBeanIdentifier("foo"), "bar"));
    }

    static void assertEquals(SerializableClientProxy proxy1, SerializableClientProxy proxy2) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(SerializableClientProxy.class, MethodHandles.privateLookupIn(SerializableClientProxy.class, MethodHandles.lookup()));
            for (Map.Entry<String, Class<?>> entry : Map.<String, Class<?>>of("beanId", BeanIdentifier.class, "contextId", String.class).entrySet()) {
                Function<SerializableClientProxy, Object> handle = Function.invoke(lookup.findGetter(SerializableClientProxy.class, entry.getKey(), entry.getValue()));
                Assertions.assertThat(handle.apply(proxy1)).isEqualTo(handle.apply(proxy2));
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
