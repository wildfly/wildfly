/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim.backed;

import java.io.IOException;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.SharedObjectCache;
import org.junit.Test;
import org.wildfly.clustering.weld.BeanManagerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshallerTestCase;

/**
 * Validates marshalling of {@link BackedAnnotatedType} and its members.
 * @author Paul Ferraro
 */
public class BackedAnnotatedTypeMarshallerTestCase extends AnnotatedTypeMarshallerTestCase {

    @Test
    public void test() throws IOException {
        BeanManagerImpl manager = BeanManagerProvider.INSTANCE.apply("foo", "bar");
        SharedObjectCache objectCache = manager.getServices().get(SharedObjectCache.class);
        ReflectionCache reflectionCache = manager.getServices().get(ReflectionCache.class);

        this.test(BackedAnnotatedType.of(BackedAnnotatedTypeMarshallerTestCase.class, objectCache, reflectionCache, "foo", "bar"));
    }
}
