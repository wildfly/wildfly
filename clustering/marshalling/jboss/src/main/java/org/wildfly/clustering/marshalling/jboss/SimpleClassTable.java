/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.wildfly.clustering.marshalling.jboss;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * Simple {@link ClassTable} implementation based on an array of recognized classes.
 * @author Paul Ferraro
 */
public class SimpleClassTable implements ClassTable {

    private final Class<?>[] classes;
    private final Map<Class<?>, Writer> writers = new IdentityHashMap<>();
    private final Externalizer<Integer> indexExternalizer;

    public SimpleClassTable(Class<?>... classes) {
        this(IndexExternalizer.select(classes.length), classes);
    }

    private SimpleClassTable(Externalizer<Integer> indexExternalizer, Class<?>... classes) {
        this.indexExternalizer = indexExternalizer;
        this.classes = classes;
        for (int i = 0; i < classes.length; i++) {
            final int index = i;
            Writer writer = new Writer() {
                @Override
                public void writeClass(Marshaller output, Class<?> clazz) throws IOException {
                    indexExternalizer.writeObject(output, index);
                }
            };
            this.writers.put(classes[i], writer);
        }
    }

    @Override
    public Writer getClassWriter(Class<?> clazz) {
        return this.writers.get(clazz);
    }

    @Override
    public Class<?> readClass(Unmarshaller input) throws IOException, ClassNotFoundException {
        return this.classes[this.indexExternalizer.readObject(input)];
    }
}
