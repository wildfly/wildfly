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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Paul Ferraro
 */
interface ProtoStreamReaderContext extends ProtoStreamOperation.Context, AutoCloseable {

    ThreadLocal<ProtoStreamReaderContext> INSTANCE = new ThreadLocal<ProtoStreamReaderContext>() {
        @Override
        protected ProtoStreamReaderContext initialValue() {
            return new ProtoStreamReaderContext() {
                private final Map<Object, Boolean> objects = new IdentityHashMap<>(64);
                private final List<Object> references = new ArrayList<>();

                @Override
                public void addReference(Object object) {
                    if (this.objects.putIfAbsent(object, Boolean.TRUE) == null) {
                        this.references.add(object);
                    }
                }

                @Override
                public Object fromReference(int referenceId) {
                    return this.references.get(referenceId);
                }
            };
        }
    };

    Object fromReference(int referenceId);

    @Override
    default void close() {
        INSTANCE.remove();
    }
}
