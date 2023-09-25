/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.discovery;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.util.cache.ComputingCache;
import org.jboss.weld.util.cache.ComputingCacheBuilder;
import org.jboss.weld.util.collections.ImmutableSet;

/**
 *
 * @author Martin Kouba
 */
public class WeldClassFileServices implements ClassFileServices {

    private CompositeIndex index;

    private ComputingCache<DotName, Set<String>> annotationClassAnnotationsCache;

    private final ClassLoader moduleClassLoader;

    private class AnnotationClassAnnotationLoader implements Function<DotName, Set<String>> {
        @Override
        public Set<String> apply(DotName name) {

            ClassInfo annotationClassInfo = index.getClassByName(name);
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();

            if (annotationClassInfo != null) {
                for (DotName annotationName : annotationClassInfo.annotationsMap().keySet()) {
                    builder.add(annotationName.toString());
                }
            } else {
                try {
                     Class<?> annotationClass = moduleClassLoader.loadClass(name.toString());
                     for (Annotation annotation : annotationClass.getDeclaredAnnotations()) {
                         builder.add(annotation.annotationType().getName());
                     }
                } catch (ClassNotFoundException e) {
                    WeldLogger.DEPLOYMENT_LOGGER.unableToLoadAnnotation(name.toString());
                }
            }
            return builder.build();
        }
    }

    /**
     *
     * @param index
     */
    public WeldClassFileServices(CompositeIndex index, ClassLoader moduleClassLoader) {
        if (index == null) {
            throw WeldLogger.ROOT_LOGGER.cannotUseAtRuntime(ClassFileServices.class.getSimpleName());
        }
        this.moduleClassLoader = moduleClassLoader;
        this.index = index;
        this.annotationClassAnnotationsCache = ComputingCacheBuilder.newBuilder().build(new AnnotationClassAnnotationLoader());
    }

    @Override
    public ClassFileInfo getClassFileInfo(String className) {
        return new WeldClassFileInfo(className, index, annotationClassAnnotationsCache, moduleClassLoader);
    }

    @Override
    public void cleanupAfterBoot() {
        if (annotationClassAnnotationsCache != null) {
            annotationClassAnnotationsCache.clear();
            annotationClassAnnotationsCache = null;
        }
        index = null;
    }

    @Override
    public void cleanup() {
        cleanupAfterBoot();
    }

}
