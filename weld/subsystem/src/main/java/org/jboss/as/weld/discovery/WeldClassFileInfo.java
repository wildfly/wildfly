/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.discovery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.Reflections;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileInfo.NestingType;
import org.jboss.weld.util.cache.ComputingCache;

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

    private final ClassInfo classInfo;

    private final CompositeIndex index;

    private final boolean isVetoed;

    private final boolean hasCdiConstructor;

    private final ComputingCache<DotName, Set<String>> annotationClassAnnotationsCache;

    private final ClassLoader classLoader;

    /**
     *
     * @param className
     * @param index
     * @param annotationClassAnnotationsCache
     */
    public WeldClassFileInfo(String className, CompositeIndex index, ComputingCache<DotName, Set<String>> annotationClassAnnotationsCache, ClassLoader classLoader) {
        this.index = index;
        this.annotationClassAnnotationsCache = annotationClassAnnotationsCache;
        this.classInfo = index.getClassByName(DotName.createSimple(className));
        if (this.classInfo == null) {
            throw WeldLogger.ROOT_LOGGER.nameNotFoundInIndex(className);
        }
        this.isVetoed = isVetoedTypeOrPackage();
        this.hasCdiConstructor = this.classInfo.hasNoArgsConstructor() || hasInjectConstructor();
        this.classLoader = classLoader;
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
        return containsAnnotation(classInfo, DotName.createSimple(annotation.getName()), annotation);
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
        return isAssignableTo(classInfo.name(), toClass);
    }

    @Override
    public boolean isVetoed() {
        return isVetoed;
    }

    @Override
    public boolean isTopLevelClass() {
        return classInfo.nestingType() == ClassInfo.NestingType.TOP_LEVEL;
    }

    @Override
    public ClassFileInfo.NestingType getNestingType() {
        NestingType result = null;
        switch (classInfo.nestingType()) {
            case ANONYMOUS:
                result = NestingType.NESTED_ANONYMOUS;
                break;
            case TOP_LEVEL:
                result = NestingType.TOP_LEVEL;
                break;
            case LOCAL:
                result = NestingType.NESTED_LOCAL;
                break;
            case INNER:
                if (Modifier.isStatic(classInfo.flags())) {
                    result = NestingType.NESTED_STATIC;
                } else {
                    result = NestingType.NESTED_INNER;
                }
                break;
            default:
                // should never happer
                break;
        }
        return result;
    }

    @Override
    public String getSuperclassName() {
        return classInfo.superName().toString();
    }

    private boolean isVetoedTypeOrPackage() {

        if (isAnnotationDeclared(classInfo, DOT_NAME_VETOED)) {
            return true;
        }

        final DotName packageInfoName = DotName.createComponentized(getPackageName(classInfo.name()), PACKAGE_INFO_NAME);
        ClassInfo packageInfo = index.getClassByName(packageInfoName);

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

    private DotName getPackageName(DotName name) {
        if (name.isComponentized()) {
            while (name.isInner()) {
                name = name.prefix();
                if (name == null) {
                    throw new IllegalStateException("Could not determine package from corrupted class name");
                }
            }
            return name.prefix();
        } else {
            final int lastIndex = name.local().lastIndexOf(".");
            if (lastIndex == -1) {
                return name;
            }
            return DotName.createSimple(name.local().substring(0, name.local().lastIndexOf(".")));
        }
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
     * @param to
     * @param name
     * @return <code>true</code> if the name is equal to the fromName, or if the name represents a superclass or superinterface of the fromName,
     *         <code>false</code> otherwise
     */
    private boolean isAssignableTo(DotName name, Class<?> to) {
        if (to.getName().equals(name.toString())) {
            return true;
        }
        if (OBJECT_NAME.equals(name)) {
            return false; // there's nothing assignable from Object.class except for Object.class
        }

        ClassInfo fromClassInfo = index.getClassByName(name);
        if (fromClassInfo == null) {
            // We reached a class that is not in the index. Let's use reflection.
            final Class<?> clazz = loadClass(name.toString());
            return to.isAssignableFrom(clazz);
        }

        DotName superName = fromClassInfo.superName();

        if (superName != null && isAssignableTo(superName, to)) {
            return true;
        }

        if (fromClassInfo.interfaces() != null) {
            for (DotName interfaceName : fromClassInfo.interfaces()) {
                if (isAssignableTo(interfaceName, to)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAnnotation(ClassInfo classInfo, DotName requiredAnnotationName, Class<? extends Annotation> requiredAnnotation) {
        // Type and members
        if (classInfo.annotations().containsKey(requiredAnnotationName)) {
            return true;
        }
        // Meta-annotations
        for (DotName annotation : classInfo.annotations().keySet()) {
            if (annotationClassAnnotationsCache.getValue(annotation).contains(requiredAnnotationName.toString())) {
                return true;
            }
        }
        // Superclass
        final DotName superName = classInfo.superName();

        if (superName != null && !OBJECT_NAME.equals(superName)) {
            final ClassInfo superClassInfo = index.getClassByName(superName);
            if (superClassInfo == null) {
                // we are accessing a class that is outside of the jandex index
                // fallback to using reflection
                return Reflections.containsAnnotation(loadClass(superName.toString()), requiredAnnotation);
            }
            if (containsAnnotation(superClassInfo, requiredAnnotationName, requiredAnnotation)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> loadClass(String className) {
        WeldLogger.DEPLOYMENT_LOGGER.tracef("Falling back to reflection for %s", className);
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw WeldLogger.ROOT_LOGGER.cannotLoadClass(className, e);
        }
    }

    @Override
    public String toString() {
        return classInfo.toString();
    }
}
