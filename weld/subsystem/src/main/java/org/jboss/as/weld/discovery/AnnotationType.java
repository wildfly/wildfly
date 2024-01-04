/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.discovery;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.function.Function;

import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public class AnnotationType {

    public static final Function<AnnotationType, String> TO_FQCN = new Function<AnnotationType, String>() {
        @Override
        public String apply(AnnotationType input) {
            return input.name.toString();
        }
    };

    public static final Function<ClassInfo, AnnotationType> FOR_CLASSINFO = new Function<ClassInfo, AnnotationType>() {
        @Override
        public AnnotationType apply(ClassInfo clazz) {
            return new AnnotationType(clazz.name(), clazz.annotationsMap().containsKey(Indices.INHERITED_NAME));
        }
    };

    private final DotName name;
    private final boolean inherited;

    public AnnotationType(DotName name, boolean inherited) {
        this.name = name;
        this.inherited = inherited;
    }

    public AnnotationType(Class<? extends Annotation> annotation) {
        this.name = DotName.createSimple(annotation.getName());
        this.inherited = annotation.isAnnotationPresent(Inherited.class);
    }

    public DotName getName() {
        return name;
    }

    public boolean isInherited() {
        return inherited;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AnnotationType) {
            AnnotationType that = (AnnotationType) obj;
            return this.name.equals(that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return "AnnotationType [name=" + name + ", inherited=" + inherited + "]";
    }
}