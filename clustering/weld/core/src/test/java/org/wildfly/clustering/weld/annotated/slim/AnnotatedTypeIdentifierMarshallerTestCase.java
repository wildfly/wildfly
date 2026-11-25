/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link AnnotatedTypeIdentifier}.
 * @author Paul Ferraro
 */
public class AnnotatedTypeIdentifierMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        BeanManagerProvider.INSTANCE.apply("foo", "bar");
        Tester<AnnotatedTypeIdentifier> tester = factory.createTester();
        tester.accept(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar"));
        tester.accept(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar", "blah"));
        tester.accept(AnnotatedTypeIdentifier.forModifiedAnnotatedType(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar")));
    }
}
