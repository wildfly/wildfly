/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim.unbacked;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.weld.BeanManagerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshallerTestCase;

import jakarta.enterprise.inject.spi.Annotated;

/**
 * Validates marshalling of {@link BackedAnnotatedType}.
 * @author Paul Ferraro
 */
public class UnbackedAnnotatedTypeMarshallerTestCase extends AnnotatedTypeMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        BeanManagerImpl manager = BeanManagerProvider.INSTANCE.apply("foo", "bar");
        SharedObjectCache objectCache = manager.getServices().get(SharedObjectCache.class);
        ReflectionCache reflectionCache = manager.getServices().get(ReflectionCache.class);

        BackedAnnotatedType<UnbackedAnnotatedTypeMarshallerTestCase> type = BackedAnnotatedType.of(UnbackedAnnotatedTypeMarshallerTestCase.class, objectCache, reflectionCache, "foo", "bar");
        ClassTransformer transformer = ClassTransformer.instance(manager);
        Tester<Annotated> tester = factory.createTester();
        this.test(tester, transformer.getUnbackedAnnotatedType(type, "bar", null));
        this.test(tester, transformer.getUnbackedAnnotatedType(type, type));
    }
}
