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