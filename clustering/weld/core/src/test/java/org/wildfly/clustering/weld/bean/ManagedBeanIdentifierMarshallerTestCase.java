/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.bean;

import java.io.IOException;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.ManagedBeanIdentifier;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link ManagedBeanIdentifier}.
 * @author Paul Ferraro
 */
public class ManagedBeanIdentifierMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanManagerProvider.INSTANCE.apply("foo", "bar");
        Tester<ManagedBeanIdentifier> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar")));
        tester.test(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar", "blah")));
        tester.test(new ManagedBeanIdentifier(AnnotatedTypeIdentifier.forModifiedAnnotatedType(AnnotatedTypeIdentifier.forBackedAnnotatedType("foo", String.class, String.class, "bar"))));
    }
}
