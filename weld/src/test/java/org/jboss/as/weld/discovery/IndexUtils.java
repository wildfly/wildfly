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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

class IndexUtils {

    static CompositeIndex createIndex(Class<?>... classes) throws IOException {
        final ClassLoader classLoader = IndexUtils.class.getClassLoader();
        final Indexer indexer = new Indexer();
        for (Class<?> javaClass : classes) {
            addClass(javaClass, indexer, classLoader);
        }
        final Index index = indexer.complete();
        return new CompositeIndex(Collections.singleton(index));
    }

    private static void addClass(Class<?> javaClass, Indexer indexer, ClassLoader classLoader) throws IOException {

        final String resourceName = javaClass.getName().replace(".", File.separator) + ".class";
        indexer.index(classLoader.getResourceAsStream(resourceName));
        for (Class<?> innerClass : javaClass.getDeclaredClasses()) {
            addClass(innerClass, indexer, classLoader);
        }
    }
}
