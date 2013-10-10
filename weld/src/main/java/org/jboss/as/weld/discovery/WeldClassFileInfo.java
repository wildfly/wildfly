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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldMessages;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.weld.resources.spi.ClassFileInfo;

import com.google.common.cache.LoadingCache;

/**
 *
 * @author Martin Kouba
 */
public class WeldClassFileInfo implements ClassFileInfo {

    private static final DotName DOT_NAME_INJECT = DotName.createSimple(Inject.class.getName());

    private static final DotName DOT_NAME_VETOED = DotName.createSimple(Vetoed.class.getName());

    private static final DotName OBJECT_NAME = DotName.createSimple(Object.class.getName());

    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    private static final String PACKAGE_INFO_NAME = "package-info";

    private static final String DOT_SEPARATOR = ".";

    private final ClassInfo classInfo;

    private final CompositeIndex index;

    private final boolean isVetoed;

    private final boolean hasCdiConstructor;

    private final LoadingCache<DotName, Set<String>> annotationClassAnnotationsCache;

    /**
     *
     * @param className
     * @param index
     * @param annotationClassAnnotationsCache
     */
    public WeldClassFileInfo(String className, CompositeIndex index, LoadingCache<DotName, Set<String>> annotationClassAnnotationsCache) {
        this.index = index;
        this.annotationClassAnnotationsCache = annotationClassAnnotationsCache;
        this.classInfo = index.getClassByName(DotName.createSimple(className));
        if (this.classInfo == null) {
            throw WeldMessages.MESSAGES.nameNotFoundInIndex(className);
        }
        this.isVetoed = isVetoedTypeOrPackage();
        this.hasCdiConstructor = this.classInfo.hasNoArgsConstructor() || hasInjectConstructor();
    }

    @Override
    public String getClassName() {
        return classInfo.name().toString();
    }

    @Override
    public boolean isAnnotationDeclared(Class<? extends Annotation> annotation) {
        return isAnnotationDeclared(classInfo, annotation);
    }

    @Override
    public boolean containsAnnotation(Class<? extends Annotation> annotation) {
        return containsAnnotation(classInfo, DotName.createSimple(annotation.getName()));
    }

    @Override
    public int getModifiers() {
        return classInfo.flags();
    }

    @Override
    public boolean hasCdiConstructor() {
        return hasCdiConstructor;
    }

    @Override
    public boolean isAssignableFrom(Class<?> fromClass) {
        return isAssignableFrom(getClassName(), fromClass);
    }

    @Override
    public boolean isAssignableTo(Class<?> toClass) {
        return isAssignableTo(DotName.createSimple(toClass.getName()), classInfo.name());
    }

    @Override
    public boolean isVetoed() {
        return isVetoed;
    }

    @Override
    public boolean isTopLevelClass() {
        // TODO This is not portable per the JSL
        // TODO Modify jandex to contain isTopLevelClass attribute
        return !classInfo.name().local().contains("$");
    }

    @Override
    public String getSuperclassName() {
        return classInfo.superName().toString();
    }

    private boolean isVetoedTypeOrPackage() {

        if (isAnnotationDeclared(classInfo, DOT_NAME_VETOED)) {
            return true;
        }

        ClassInfo packageInfo = index.getClassByName(DotName.createSimple(getPackageName(classInfo.name()) + DOT_SEPARATOR + PACKAGE_INFO_NAME));

        if (packageInfo != null && isAnnotationDeclared(packageInfo, DOT_NAME_VETOED)) {
            return true;
        }
        return false;
    }

    private boolean isAnnotationDeclared(ClassInfo classInfo, Class<? extends Annotation> annotation) {
        return isAnnotationDeclared(classInfo, DotName.createSimple(annotation.getName()));
    }

    private boolean isAnnotationDeclared(ClassInfo classInfo, DotName requiredAnnotationName) {
        List<AnnotationInstance> annotations = classInfo.annotations().get(requiredAnnotationName);
        if (annotations != null) {
            for (AnnotationInstance annotationInstance : annotations) {
                if (annotationInstance.target().equals(classInfo)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasInjectConstructor() {
        List<AnnotationInstance> annotationInstances = classInfo.annotations().get(DOT_NAME_INJECT);
        if (annotationInstances != null) {
            for (AnnotationInstance instance : annotationInstances) {
                AnnotationTarget target = instance.target();
                if (target instanceof MethodInfo) {
                    MethodInfo methodInfo = (MethodInfo) target;
                    if (methodInfo.name().equals(CONSTRUCTOR_METHOD_NAME)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getPackageName(DotName name) {
        // TODO https://issues.jboss.org/browse/JANDEX-20
        // String packageName;
        // if (name.isComponentized()) {
        // packageName = name.prefix().toString();
        // } else {
        // packageName = name.local().substring(0, name.local().lastIndexOf("."));
        // }
        // return packageName;
        return name.toString().substring(0, name.toString().lastIndexOf("."));
    }

    /**
     * @param className
     * @param fromClass
     * @return
     */
    private boolean isAssignableFrom(String className, Class<?> fromClass) {
        if (className.equals(fromClass.getName())) {
            return true;
        }
        if (Object.class.equals(fromClass)) {
            return false; // there's nothing assignable from Object.class except for Object.class
        }

        Class<?> superClass = fromClass.getSuperclass();

        if (superClass != null && isAssignableFrom(className, superClass)) {
            return true;
        }

        for (Class<?> interfaceClass : fromClass.getInterfaces()) {
            if (isAssignableFrom(className, interfaceClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param name
     * @param fromName
     * @return <code>true</code> if the name is equal to the fromName, or if the name represents a superclass or superinterface of the fromName,
     *         <code>false</code> otherwise
     */
    private boolean isAssignableTo(DotName name, DotName fromName) {
        if (name.equals(fromName)) {
            return true;
        }
        if (OBJECT_NAME.equals(fromName)) {
            return false; // there's nothing assignable from Object.class except for Object.class
        }

        ClassInfo fromClassInfo = index.getClassByName(fromName);
        if (fromClassInfo == null) {
            throw WeldMessages.MESSAGES.nameNotFoundInIndex(fromName.toString());
        }

        DotName superName = fromClassInfo.superName();

        if (superName != null && isAssignableTo(name, superName)) {
            return true;
        }

        if (fromClassInfo.interfaces() != null) {
            for (DotName interfaceName : fromClassInfo.interfaces()) {
                if (isAssignableTo(name, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAnnotation(ClassInfo classInfo, DotName requiredAnnotationName) {
        // Type and members
        if (classInfo.annotations().containsKey(requiredAnnotationName)) {
            return true;
        }
        // Meta-annotations
        for (DotName annotation : classInfo.annotations().keySet()) {
            try {
                if (annotationClassAnnotationsCache.get(annotation).contains(requiredAnnotationName.toString())) {
                    return true;
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        // Superclass
        final DotName superName = classInfo.superName();

        if (superName != null && !OBJECT_NAME.equals(superName)) {
            final ClassInfo superClassInfo = index.getClassByName(superName);
            if (superClassInfo == null) {
                throw WeldMessages.MESSAGES.nameNotFoundInIndex(superName.toString());
            }
            if (containsAnnotation(superClassInfo, requiredAnnotationName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return classInfo.toString();
    }
}
