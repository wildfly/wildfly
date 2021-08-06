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
interface ProtoStreamWriterContext extends ProtoStreamOperation.Context {

    interface Factory extends Function<ProtoStreamWriter, ProtoStreamWriterContext>, AutoCloseable {
        @Override
        default void close() {
            FACTORY.remove();
        }
    }

    ThreadLocal<Factory> FACTORY = new ThreadLocal<Factory>() {
        @Override
        protected Factory initialValue() {
            return new Factory() {
                private final Map<Class<?>, ProtoStreamWriterContext> contexts = new IdentityHashMap<>(2);

                @Override
                public ProtoStreamWriterContext apply(ProtoStreamWriter writer) {
                    return this.contexts.computeIfAbsent(writer.getClass(), Context::new);
                }

                class Context implements ProtoStreamWriterContext, Function<Object, Integer> {
                    private Map<Object, Integer> references = new IdentityHashMap<>(64);
                    private int index = 0;

                    Context(Class<?> targetClass) {
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
                }
            };
        }
    };

    Integer getReference(Object object);
}
