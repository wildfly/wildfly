/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.discovery;

import java.io.IOException;
import java.util.Collections;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

class IndexUtils {

    static CompositeIndex createIndex(Object... resources) throws IOException {
        final ClassLoader classLoader = IndexUtils.class.getClassLoader();
        final Indexer indexer = new Indexer();
        for (Object resource : resources) {
            addResource(resource, indexer, classLoader);
        }
        final Index index = indexer.complete();
        return new CompositeIndex(Collections.singleton(index));
    }

    private static void addResource(Object resource, Indexer indexer, ClassLoader classLoader) throws IOException {
        final String resourceName;
        if (resource instanceof Class<?>) {
            resourceName = ((Class<?>) resource).getName().replace(".", "/") + ".class";
        } else if (resource instanceof String) {
            resourceName = resource.toString();
        } else {
            throw new IllegalArgumentException("Unsupported resource type");
        }
        indexer.index(classLoader.getResourceAsStream(resourceName));
        if (resource instanceof Class<?>) {
            for (Class<?> innerClass : ((Class<?>) resource).getDeclaredClasses()) {
                addResource(innerClass, indexer, classLoader);
            }
        }
    }
}
