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
import java.lang.annotation.Inherited;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldMessages;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.weld.bootstrap.api.BootstrapService;
import org.jboss.weld.resources.spi.AnnotationDiscovery;
import org.jboss.weld.resources.spi.ExtendedAnnotationDiscovery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;

/**
 * Implementation of {@link ExtendedAnnotationDiscovery} that uses composite Jandex index.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldAnnotationDiscovery implements ExtendedAnnotationDiscovery, BootstrapService {

    private static final DotName INHERITED_NAME = DotName.createSimple(Inherited.class.getName());

    private class LoadAffectedClassNames extends CacheLoader<Class<? extends Annotation>, Set<String>> {
        @Override
        public Set<String> load(Class<? extends Annotation> key) throws Exception {
            // we enumerate a set of all the annotations annotated by the given meta-annotation plus the given annotation itself
            Set<AnnotationType> firstLevelAnnotations = new HashSet<AnnotationType>(annotatedAnnotations.get(key));
            firstLevelAnnotations.add(new AnnotationType(DotName.createSimple(key.getName()), key.isAnnotationPresent(Inherited.class)));

            Collection<ClassInfo> affectedClasses = helper.getAffectedClasses(firstLevelAnnotations);
            return ImmutableSet.copyOf(Collections2.transform(affectedClasses, Indices.CLASS_INFO_TO_FQCN));
        }
    }

    private class LoadAnnotatedAnnotations extends CacheLoader<Class<? extends Annotation>, Set<AnnotationType>> {
        @Override
        public Set<AnnotationType> load(Class<? extends Annotation> key) throws Exception {
            ImmutableSet.Builder<AnnotationType> builder = ImmutableSet.builder();
            for (AnnotationInstance instance : index.getAnnotations(DotName.createSimple(key.getName()))) {
                AnnotationTarget target = instance.target();
                if (target instanceof ClassInfo) {
                    ClassInfo clazz = (ClassInfo) target;
                    if (Indices.isAnnotation(clazz)) {
                        builder.add(new AnnotationType(clazz.name(), clazz.annotations().containsKey(INHERITED_NAME)));
                    }
                }
            }
            return builder.build();
        }
    }

    private CompositeIndex index;
    private final RequiredAnnotationTargetDiscovery helper;

    // caching
    private final LoadingCache<Class<? extends Annotation>, Set<String>> affectedClasses = CacheBuilder.newBuilder().build(new LoadAffectedClassNames());
    private final LoadingCache<Class<? extends Annotation>, Set<AnnotationType>> annotatedAnnotations = CacheBuilder.newBuilder().build(new LoadAnnotatedAnnotations());

    public WeldAnnotationDiscovery(CompositeIndex index) {
        this.index = index;
        this.helper = new RequiredAnnotationTargetDiscovery(IndexAdapter.forCompositeIndex(index));
    }

    @Override
    public boolean containsAnnotation(Class<?> javaClass, Class<? extends Annotation> annotation) {
        if (index == null) {
            throw WeldMessages.MESSAGES.cannotUseAtRuntime(AnnotationDiscovery.class.getSimpleName());
        }
        try {
            return affectedClasses.get(annotation).contains(javaClass.getName());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getAnnotationsAnnotatedWith(Class<? extends Annotation> annotation) {
        if (index == null) {
            throw WeldMessages.MESSAGES.cannotUseAtRuntime(ExtendedAnnotationDiscovery.class.getSimpleName());
        }
        try {
            return ImmutableSet.copyOf(Collections2.transform(annotatedAnnotations.get(annotation), AnnotationType.TO_FQCN));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanupAfterBoot() {
        this.affectedClasses.cleanUp();
        this.annotatedAnnotations.cleanUp();
        this.index = null;
    }

    @Override
    public void cleanup() {
        cleanupAfterBoot();
    }
}
