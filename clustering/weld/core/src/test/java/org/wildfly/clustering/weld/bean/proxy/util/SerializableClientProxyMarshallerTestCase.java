/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.proxy.util;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.Set;

import org.jboss.weld.bean.StringBeanIdentifier;
import org.jboss.weld.bean.proxy.util.SerializableClientProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.security.manager.WildFlySecurityManager;

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
        for (String field : Set.of("beanId", "contextId")) {
            Assertions.assertEquals(readField(proxy1, field), readField(proxy2, field));
        }
    }

    private static Object readField(Object object, String fieldName) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Object run() {
                try {
                    Field field = object.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(object);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
