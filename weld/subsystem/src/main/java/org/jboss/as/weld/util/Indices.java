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
package org.jboss.as.weld.util;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Utilities for working with Jandex indices.
 *
 * @author Jozef Hartinger
 *
 */
public class Indices {

    public static final DotName INHERITED_NAME = DotName.createSimple(Inherited.class.getName());

    public static final Function<ClassInfo, String> CLASS_INFO_TO_FQCN = new Function<ClassInfo, String>() {
        @Override
        public String apply(ClassInfo input) {
            return input.name().toString();
        }
    };

    public static final Predicate<ClassInfo> ANNOTATION_PREDICATE = new Predicate<ClassInfo>() {
        @Override
        public boolean test(ClassInfo input) {
            return isAnnotation(input);
        }
    };

    private Indices() {
    }

    private static final int ANNOTATION = 0x00002000;

    public static boolean isAnnotation(ClassInfo clazz) {
        return (clazz.flags() & ANNOTATION) != 0;
    }

    /**
     * Determines a list of classes the given annotation instances are defined on. If an annotation instance is not defined on a
     * class (e.g. on a member) this annotation instance is not reflected anyhow in the resulting list.
     */
    public static List<ClassInfo> getAnnotatedClasses(List<AnnotationInstance> instances) {
        List<ClassInfo> result = new ArrayList<ClassInfo>();
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                result.add((ClassInfo) target);
            }
        }
        return result;
    }
}
