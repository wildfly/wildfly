/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.ManagedBeanIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link ManagedBeanIdentifier}.
 * @author Paul Ferraro
 */
public class ManagedBeanIdentifierMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        BeanManagerProvider.INSTANCE.apply("foo", "bar");
        Tester<ManagedBeanIdentifier> tester = factory.createTester();
        tester.accept(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar")));
        tester.accept(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar", "blah")));
        tester.accept(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forModifiedAnnotatedType(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar"))));
    }
}
