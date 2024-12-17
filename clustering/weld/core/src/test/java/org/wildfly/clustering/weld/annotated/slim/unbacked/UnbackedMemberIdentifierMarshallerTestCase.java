/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim.unbacked;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.annotated.slim.unbacked.UnbackedMemberIdentifier;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link UnbackedMemberIdentifier}.
 * @author Paul Ferraro
 */
public class UnbackedMemberIdentifierMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        BeanManagerImpl manager = BeanManagerProvider.INSTANCE.apply("foo", "bar");
        SharedObjectCache objectCache = manager.getServices().get(SharedObjectCache.class);
        ReflectionCache reflectionCache = manager.getServices().get(ReflectionCache.class);

        BackedAnnotatedType<UnbackedMemberIdentifierMarshallerTestCase> type = BackedAnnotatedType.of(UnbackedMemberIdentifierMarshallerTestCase.class, objectCache, reflectionCache, "foo", "bar");
        ClassTransformer transformer = ClassTransformer.instance(manager);

        Tester<UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase>> tester = factory.createTester(UnbackedMemberIdentifierMarshallerTestCase::assertEquals);
        tester.accept(new UnbackedMemberIdentifier<>(transformer.getUnbackedAnnotatedType(type, type), "memberId"));
    }

    static void assertEquals(UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase> identifier1, UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase> identifier2) {
        Assertions.assertEquals(identifier1.getMemberId(), identifier2.getMemberId());
        Assertions.assertEquals(identifier1.getType(), identifier2.getType());
    }
}
