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
package org.jboss.as.weld.util;

import org.jboss.jandex.ClassInfo;

import com.google.common.base.Function;

/**
 * Utilities for working with Jandex indices.
 *
 * @author Jozef Hartinger
 *
 */
public class Indices {

    public static final Function<ClassInfo, String> CLASS_INFO_TO_FQCN = new Function<ClassInfo, String>() {
        @Override
        public String apply(ClassInfo input) {
            return input.name().toString();
        }
    };

    private Indices() {
    }

    private static final int ANNOTATION = 0x00002000;

    public static boolean isAnnotation(ClassInfo clazz) {
        return (clazz.flags() & ANNOTATION) != 0;
    }
}
