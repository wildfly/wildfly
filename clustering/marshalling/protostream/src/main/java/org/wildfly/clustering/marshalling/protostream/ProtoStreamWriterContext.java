/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Paul Ferraro
 */
interface ProtoStreamWriterContext extends ProtoStreamOperation.Context, AutoCloseable {

    interface Factory extends Function<ProtoStreamWriter, ProtoStreamWriterContext>, AutoCloseable {
        @Override
        default void close() {
            FACTORY.remove();
        }
    }

    ThreadLocal<Factory> FACTORY = new ThreadLocal<Factory>() {
        @Override
        protected Factory initialValue() {
            return new DefaultFactory();
        }

        class DefaultFactory implements Factory {
            final Map<Class<?>, ProtoStreamWriterContext> contexts = new IdentityHashMap<>(2);

            @Override
            public ProtoStreamWriterContext apply(ProtoStreamWriter writer) {
                return this.contexts.computeIfAbsent(writer.getClass(), DefaultProtoStreamWriterContext::new);
            }

            class DefaultProtoStreamWriterContext implements ProtoStreamWriterContext, Function<Object, Integer> {
                private final Class<?> writerClass;
                private final Map<Object, Integer> references = new IdentityHashMap<>(64);
                private int index = 0;

                DefaultProtoStreamWriterContext(Class<?> targetClass) {
                    this.writerClass = targetClass;
                }

                @Override
                public Integer getReference(Object object) {
                    return this.references.get(object);
                }

                @Override
                public void addReference(Object object) {
                    this.references.computeIfAbsent(object, this);
                }

                @Override
                public Integer apply(Object key) {
                    return this.index++;
                }

                @Override
                public void close() {
                    DefaultFactory.this.contexts.remove(this.writerClass);
                }
            }
        }
    };

    Integer getReference(Object object);

    @Override
    void close();
}
