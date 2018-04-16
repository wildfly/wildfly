/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.extension.classchange;


import java.util.List;

import org.jboss.jandex.ClassInfo;

/**
 * A listener that gets notified when classes from a deployment are replaced
 */
public interface ClassChangeListener {

    void classesReplaced(List<ChangedClasssDefinition> replacedClasses, List<NewClassDefinition> newClassDefinitions);


    class ChangedClasssDefinition {

        private final Class<?> javaClass;
        private final ClassInfo classInfo;

        public ChangedClasssDefinition(Class<?> javaClass, ClassInfo classInfo) {
            this.javaClass = javaClass;
            this.classInfo = classInfo;
        }

        public Class<?> getJavaClass() {
            return javaClass;
        }

        public ClassInfo getClassInfo() {
            return classInfo;
        }
    }

    class NewClassDefinition {

        private final String name;
        private final ClassLoader classLoader;
        private final byte[] data;
        private final ClassInfo classInfo;

        public NewClassDefinition(String name, ClassLoader classLoader, byte[] data, ClassInfo classInfo) {
            this.name = name;
            this.classLoader = classLoader;
            this.data = data;
            this.classInfo = classInfo;
        }

        public String getName() {
            return name;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public byte[] getData() {
            return data;
        }

        public ClassInfo getClassInfo() {
            return classInfo;
        }
    }
}
