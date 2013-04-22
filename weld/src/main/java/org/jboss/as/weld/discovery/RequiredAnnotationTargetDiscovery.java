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

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

public class RequiredAnnotationTargetDiscovery {

    private static final String CONSTRUCTOR_NAME = "<init>";

    private final IndexAdapter index;

    public RequiredAnnotationTargetDiscovery(IndexAdapter index) {
        this.index = index;
    }

    /**
     * Returns a list of classes that contain any of the given annotations as defined in CDI 1.1 - 11.4
     */
    public List<ClassInfo> getAffectedClasses(Iterable<AnnotationType> annotations) {
        List<ClassInfo> classes = new ArrayList<ClassInfo>();
        for (AnnotationType annotation : annotations) {
            getAffectedClasses(classes, annotation);
        }
        return classes;
    }

    protected List<ClassInfo> getAffectedClasses(List<ClassInfo> classes, AnnotationType annotation) {
        for (AnnotationInstance instance : index.getAnnotations(annotation.getName())) {
            AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                processTopLevelClass(classes, (ClassInfo) target, annotation.isInherited());
            } else if (target instanceof FieldInfo) {
                processField(classes, (FieldInfo) target);
            } else if (target instanceof MethodInfo) {
                MethodInfo method = (MethodInfo) target;
                if (CONSTRUCTOR_NAME.equals(method.name())) {
                    processConstructor(classes, method);
                } else {
                    processMethod(classes, method);
                }
            } else if (target instanceof MethodParameterInfo) {
                processParameter(classes, (MethodParameterInfo) target);
            }
        }
        return classes;
    }

    protected void processTopLevelClass(List<ClassInfo> classes, ClassInfo clazz, boolean inherited) {
        classes.add(clazz);
        if (inherited) {
            processSubclasses(classes, clazz);
        }
    }

    protected void processSubclasses(List<ClassInfo> classes, ClassInfo clazz) {
        for (ClassInfo subclass : index.getKnownDirectSubclasses(clazz.name())) {
            classes.add(subclass);
            processSubclasses(classes, subclass);
        }

    }

    protected void processField(List<ClassInfo> classes, FieldInfo field) {
        classes.add(field.declaringClass());
        processSubclasses(classes, field.declaringClass());
    }

    protected void processMethod(List<ClassInfo> classes, MethodInfo method) {
        classes.add(method.declaringClass());
        processSubclasses(classes, method.declaringClass());
    }

    protected void processConstructor(List<ClassInfo> classes, MethodInfo method) {
        processMethod(classes, method);
    }

    protected void processParameter(List<ClassInfo> classes, MethodParameterInfo parameter) {
        classes.add(parameter.method().declaringClass());
        processSubclasses(classes, parameter.method().declaringClass());
    }
}
