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

import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.google.common.base.Function;

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
            return new AnnotationType(clazz.name(), clazz.annotations().containsKey(Indices.INHERITED_NAME));
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