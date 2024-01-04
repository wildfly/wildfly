/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.annotations.repository.jandex;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jca.common.spi.annotations.repository.Annotation;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;

/**
 *
 * An AnnotationRepositoryImpl.
 *
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 *
 */
public class JandexAnnotationRepositoryImpl implements AnnotationRepository {

    private final Index backingRepository;

    private final ClassLoader cl;

    /**
     *
     * Create a new AnnotationRepositoryImpl using papaki backend
     *
     * @param backingRepository the caking papaki repository
     * @param cl classLoader
     * @throws IllegalArgumentException in case pas sed repository is null
     */
    public JandexAnnotationRepositoryImpl(Index backingRepository, ClassLoader cl) throws IllegalArgumentException {
        this.backingRepository = checkNotNullParam("backingRepository", backingRepository);
        this.cl = cl;
    }

    @Override
    public Collection<Annotation> getAnnotation(Class<?> annotationClass) {
        List<AnnotationInstance> instances = backingRepository.getAnnotations(DotName.createSimple(annotationClass
                .getName()));
        ArrayList<Annotation> annotations = new ArrayList<Annotation>(instances.size());
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            Annotation annotation = null;
            if (target instanceof MethodInfo) {
                MethodInfo m = (MethodInfo) target;
                List<String> parameterTypes = new ArrayList<String>(m.args().length);
                for (Type type : m.args()) {
                    parameterTypes.add(type.toString());
                }
                String declaringClass = m.declaringClass().name().toString();
                annotation = new AnnotationImpl(declaringClass, cl, parameterTypes, m.name(), true, false, annotationClass);
            }
            if (target instanceof FieldInfo) {
                FieldInfo f = (FieldInfo) target;
                String declaringClass = f.declaringClass().name().toString();
                annotation = new AnnotationImpl(declaringClass, cl, null, f.name(), false, true, annotationClass);
            }
            if (target instanceof ClassInfo) {
                ClassInfo c = (ClassInfo) target;
                annotation = new AnnotationImpl(c.name().toString(), cl, null, null, false, false, annotationClass);
            }
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        annotations.trimToSize();
        if (annotations.isEmpty()) {
            return null;
        } else {
            return Collections.unmodifiableList(annotations);
        }

    }
}
