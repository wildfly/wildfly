/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.discovery;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Martin Kouba
 */
public class WeldClassFileServices implements ClassFileServices {

    private CompositeIndex index;

    private LoadingCache<DotName, Set<String>> annotationClassAnnotationsCache;

    private final ClassLoader moduleClassLoader;

    private class AnnotationClassAnnotationLoader extends CacheLoader<DotName, Set<String>> {
        @Override
        public Set<String> load(DotName name) throws Exception {

            ClassInfo annotationClassInfo = index.getClassByName(name);
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();

            if (annotationClassInfo != null) {
                for (DotName annotationName : annotationClassInfo.annotations().keySet()) {
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
        this.annotationClassAnnotationsCache = CacheBuilder.newBuilder().build(new AnnotationClassAnnotationLoader());
    }

    @Override
    public ClassFileInfo getClassFileInfo(String className) {
        return new WeldClassFileInfo(className, index, annotationClassAnnotationsCache, moduleClassLoader);
    }

    @Override
    public void cleanupAfterBoot() {
        if (annotationClassAnnotationsCache != null) {
            annotationClassAnnotationsCache.invalidateAll();
            annotationClassAnnotationsCache = null;
        }
        index = null;
    }

    @Override
    public void cleanup() {
        cleanupAfterBoot();
    }

}
