/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.metadata;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract factory that can produce a {@link ClassAnnotationInformation}
 *
 * @author Stuart Douglas
 */
public abstract class ClassAnnotationInformationFactory<A extends Annotation, T> {


    private final Class<A> annotationType;
    private final Class<?> multiAnnotationType;
    private final DotName annotationDotName;
    private final DotName multiAnnotationDotName;


    protected ClassAnnotationInformationFactory(final Class<A> annotationType, final Class<?> multiAnnotationType) {
        this.annotationType = annotationType;
        this.annotationDotName = DotName.createSimple(annotationType.getName());
        this.multiAnnotationType = multiAnnotationType;
        if (multiAnnotationType != null) {
            this.multiAnnotationDotName = DotName.createSimple(multiAnnotationType.getName());
        } else {
            this.multiAnnotationDotName = null;
        }
    }

    public Map<String, ClassAnnotationInformation<A, T>> createAnnotationInformation(final CompositeIndex index) {

        final List<AnnotationInstance> annotations = new ArrayList<AnnotationInstance>();
        if (multiAnnotationDotName != null) {
            for (AnnotationInstance multiInstance : index.getAnnotations(multiAnnotationDotName)) {
                annotations.addAll(fromMultiAnnotation(multiInstance));
            }
        }

        final List<AnnotationInstance> simpleAnnotations = index.getAnnotations(annotationDotName);
        if (simpleAnnotations != null) {
            annotations.addAll(simpleAnnotations);
        }


        final Map<DotName, List<AnnotationInstance>> classLevel = new HashMap<DotName, List<AnnotationInstance>>();
        final Map<DotName, List<AnnotationInstance>> methodLevel = new HashMap<DotName, List<AnnotationInstance>>();
        final Map<DotName, List<AnnotationInstance>> fieldLevel = new HashMap<DotName, List<AnnotationInstance>>();
        for (AnnotationInstance instance : annotations) {
            final DotName targetClass = getAnnotationClass(instance.target()).name();
            if (instance.target() instanceof ClassInfo) {
                List<AnnotationInstance> data = classLevel.get(targetClass);
                if (data == null) classLevel.put(targetClass, data = new ArrayList<AnnotationInstance>(1));
                data.add(instance);
            } else if (instance.target() instanceof MethodInfo) {
                List<AnnotationInstance> data = methodLevel.get(targetClass);
                if (data == null) methodLevel.put(targetClass, data = new ArrayList<AnnotationInstance>(1));
                data.add(instance);
            } else if (instance.target() instanceof FieldInfo) {
                List<AnnotationInstance> data = fieldLevel.get(targetClass);
                if (data == null) fieldLevel.put(targetClass, data = new ArrayList<AnnotationInstance>(1));
                data.add(instance);
            } else if (instance.target() instanceof MethodParameterInfo) {
                //ignore for now
            } else {
                throw new RuntimeException("Unknown AnnotationTarget type: " + instance.target());
            }
        }

        final Map<String, ClassAnnotationInformation<A, T>> ret = new HashMap<String, ClassAnnotationInformation<A, T>>();

        final Set<DotName> allClasses = new HashSet<DotName>(classLevel.keySet());
        allClasses.addAll(methodLevel.keySet());
        allClasses.addAll(fieldLevel.keySet());


        for (DotName clazz : allClasses) {

            final List<AnnotationInstance> classAnnotations = classLevel.get(clazz);
            final List<T> classData;
            if (classAnnotations == null) {
                classData = Collections.emptyList();
            } else {
                classData = new ArrayList<T>(classAnnotations.size());
                for (AnnotationInstance instance : classAnnotations) {
                    classData.add(fromAnnotation(instance));
                }
            }

            final List<AnnotationInstance> fieldAnnotations = fieldLevel.get(clazz);
            final Map<String, List<T>> fieldData;
            //field level annotations
            if (fieldAnnotations == null) {
                fieldData = Collections.emptyMap();
            } else {
                fieldData = new HashMap<String, List<T>>();
                for (AnnotationInstance instance : fieldAnnotations) {
                    final String name = ((FieldInfo) instance.target()).name();
                    List<T> data = fieldData.get(name);
                    if (data == null) {
                        fieldData.put(name, data = new ArrayList<T>(1));
                    }
                    data.add(fromAnnotation(instance));
                }
            }

            final List<AnnotationInstance> methodAnnotations = methodLevel.get(clazz);
            final Map<MethodIdentifier, List<T>> methodData;
            //method level annotations
            if (methodAnnotations == null) {
                methodData = Collections.emptyMap();
            } else {
                methodData = new HashMap<MethodIdentifier, List<T>>();
                for (AnnotationInstance instance : methodAnnotations) {
                    final MethodIdentifier identifier = getMethodIdentifier(instance.target());
                    List<T> data = methodData.get(identifier);
                    if (data == null) {
                        methodData.put(identifier, data = new ArrayList<T>(1));
                    }
                    data.add(fromAnnotation(instance));
                }
            }
            ClassAnnotationInformation<A, T> information = new ClassAnnotationInformation<A, T>(annotationType, classData, methodData, fieldData);
            ret.put(clazz.toString(), information);
        }

        return ret;
    }

    private ClassInfo getAnnotationClass(final AnnotationTarget annotationTarget) {
        if (annotationTarget instanceof ClassInfo) {
            return (ClassInfo) annotationTarget;
        } else if (annotationTarget instanceof MethodInfo) {
            return ((MethodInfo) annotationTarget).declaringClass();
        } else if (annotationTarget instanceof FieldInfo) {
            return ((FieldInfo) annotationTarget).declaringClass();
        } else if (annotationTarget instanceof MethodParameterInfo) {
            return ((MethodParameterInfo) annotationTarget).method().declaringClass();
        } else {
            throw new RuntimeException("Unknown AnnotationTarget type: " + annotationTarget);
        }
    }


    protected abstract T fromAnnotation(AnnotationInstance annotationInstance);

    protected List<AnnotationInstance> fromMultiAnnotation(AnnotationInstance multiAnnotationInstance) {
        List<AnnotationInstance> instances = new ArrayList<AnnotationInstance>();
        final AnnotationInstance[] values = multiAnnotationInstance.value().asNestedArray();
        Collections.addAll(instances, values);
        return instances;
    }

    private MethodIdentifier getMethodIdentifier(final AnnotationTarget target) {
        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        final String[] args = new String[methodInfo.args().length];
        for (int i = 0; i < methodInfo.args().length; i++) {
            args[i] = methodInfo.args()[i].name().toString();
        }
        return MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), args);
    }

    public Class<A> getAnnotationType() {
        return annotationType;
    }

    public Class<?> getMultiAnnotationType() {
        return multiAnnotationType;
    }
}
