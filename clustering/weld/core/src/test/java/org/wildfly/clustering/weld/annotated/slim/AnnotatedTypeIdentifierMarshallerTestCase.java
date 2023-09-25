/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link AnnotatedTypeIdentifier}.
 * @author Paul Ferraro
 */
public class AnnotatedTypeIdentifierMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanManagerProvider.INSTANCE.apply("foo", "bar");
        Tester<AnnotatedTypeIdentifier> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar"));
        tester.test(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar", "blah"));
        tester.test(AnnotatedTypeIdentifier.forModifiedAnnotatedType(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar")));
    }
}
