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

/**
 *
 * @author Martin Kouba
 */
public class WeldClassFileInfo implements ClassFileInfo {

    private static final DotName DOT_NAME_INJECT = DotName.createSimple(Inject.class.getName());

    private static final DotName DOT_NAME_VETOED = DotName.createSimple(Vetoed.class.getName());

    private final static String CONSTRUCTOR_METHOD_NAME = "<init>";

    private final static String PACKAGE_INFO_NAME = "package-info";

    private final static String DOT_SEPARATOR = ".";

    private final ClassInfo classInfo;

    private final CompositeIndex index;

    private final boolean isVetoed;

    private final boolean hasCdiConstructor;

    /**
     *
     * @param className
     * @param index
     */
    public WeldClassFileInfo(String className, CompositeIndex index) {
        this.index = index;
        this.classInfo = index.getClassByName(DotName.createSimple(className));
        if (this.classInfo == null) {
            throw WeldMessages.MESSAGES.nameNotFoundInIndex(className);
        }
        this.isVetoed = isVetoedTypeOrPackage(index);
        this.hasCdiConstructor = this.classInfo.hasNoArgsConstructor() || hasInjectConstructor();
    }

    @Override
    public String getClassName() {
        return classInfo.name().toString();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return isAnnotationPresent(classInfo, annotation);
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
        return isNameEqualOrSuperType(classInfo.name(), DotName.createSimple(fromClass.getName()));
    }

    @Override
    public boolean isAssignableTo(Class<?> toClass) {
        return isNameEqualOrSuperType(DotName.createSimple(toClass.getName()), classInfo.name());
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

    private boolean isVetoedTypeOrPackage(CompositeIndex index) {

        if (isAnnotationPresent(classInfo, DOT_NAME_VETOED)) {
            return true;
        }

        ClassInfo packageInfo = index.getClassByName(DotName.createSimple(getPackageName(classInfo.name()) + DOT_SEPARATOR + PACKAGE_INFO_NAME));

        if (packageInfo != null && isAnnotationPresent(packageInfo, DOT_NAME_VETOED)) {
            return true;
        }
        return false;
    }

    private boolean isAnnotationPresent(ClassInfo classInfo, Class<? extends Annotation> annotation) {
        return isAnnotationPresent(classInfo, DotName.createSimple(annotation.getName()));
    }

    private boolean isAnnotationPresent(ClassInfo classInfo, DotName requiredAnnotationName) {
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
        for (AnnotationInstance instance : annotationInstances) {
            AnnotationTarget target = instance.target();
            if (target instanceof MethodInfo) {
                MethodInfo methodInfo = (MethodInfo) target;
                if (methodInfo.name().equals(CONSTRUCTOR_METHOD_NAME)) {
                    return true;
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
     * @param name
     * @param targetName
     * @return
     */
    private boolean isNameEqualOrSuperType(DotName name, DotName targetName) {
        if (name.equals(targetName)) {
            return true;
        }

        ClassInfo startClassInfo = index.getClassByName(targetName);
        if (startClassInfo == null) {
            throw WeldMessages.MESSAGES.nameNotFoundInIndex(targetName.toString());
        }

        DotName superName = startClassInfo.superName();

        if (superName != null && isNameEqualOrSuperType(name, superName)) {
            return true;
        }

        if (startClassInfo.interfaces() != null) {
            for (DotName interfaceName : startClassInfo.interfaces()) {
                if (isNameEqualOrSuperType(name, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return classInfo.toString();
    }
}
