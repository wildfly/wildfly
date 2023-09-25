/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim.unbacked;

import java.io.IOException;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.junit.Test;
import org.wildfly.clustering.weld.BeanManagerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshallerTestCase;

/**
 * Validates marshalling of {@link BackedAnnotatedType}.
 * @author Paul Ferraro
 */
public class UnbackedAnnotatedTypeMarshallerTestCase extends AnnotatedTypeMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanManagerImpl manager = BeanManagerProvider.INSTANCE.apply("foo", "bar");
        SharedObjectCache objectCache = manager.getServices().get(SharedObjectCache.class);
        ReflectionCache reflectionCache = manager.getServices().get(ReflectionCache.class);

        BackedAnnotatedType<UnbackedAnnotatedTypeMarshallerTestCase> type = BackedAnnotatedType.of(UnbackedAnnotatedTypeMarshallerTestCase.class, objectCache, reflectionCache, "foo", "bar");
        ClassTransformer transformer = ClassTransformer.instance(manager);
        this.test(transformer.getUnbackedAnnotatedType(type, "bar", null));
        this.test(transformer.getUnbackedAnnotatedType(type, type));
    }
}
