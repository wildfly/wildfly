/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim.unbacked;

import java.io.IOException;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.annotated.slim.unbacked.UnbackedMemberIdentifier;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.weld.BeanManagerProvider;

/**
 * Validates marshalling of {@link UnbackedMemberIdentifier}.
 * @author Paul Ferraro
 */
public class UnbackedMemberIdentifierMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanManagerImpl manager = BeanManagerProvider.INSTANCE.apply("foo", "bar");
        SharedObjectCache objectCache = manager.getServices().get(SharedObjectCache.class);
        ReflectionCache reflectionCache = manager.getServices().get(ReflectionCache.class);

        BackedAnnotatedType<UnbackedMemberIdentifierMarshallerTestCase> type = BackedAnnotatedType.of(UnbackedMemberIdentifierMarshallerTestCase.class, objectCache, reflectionCache, "foo", "bar");
        ClassTransformer transformer = ClassTransformer.instance(manager);

        Tester<UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new UnbackedMemberIdentifier<>(transformer.getUnbackedAnnotatedType(type, type), "memberId"), UnbackedMemberIdentifierMarshallerTestCase::assertEquals);
    }

    static void assertEquals(UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase> identifier1, UnbackedMemberIdentifier<UnbackedMemberIdentifierMarshallerTestCase> identifier2) {
        Assert.assertEquals(identifier1.getMemberId(), identifier2.getMemberId());
        Assert.assertEquals(identifier1.getType(), identifier2.getType());
    }
}
