/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
