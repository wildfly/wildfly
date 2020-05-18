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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Ferraro
 */
public interface ProtoStreamReaderContext extends AutoCloseable {

    ThreadLocal<ProtoStreamReaderContext> INSTANCE = new ThreadLocal<ProtoStreamReaderContext>() {
        @Override
        protected ProtoStreamReaderContext initialValue() {
            return new ProtoStreamReaderContext() {
                private final Map<Integer, Object> objects = new HashMap<>();
                private int index = 0;

                @Override
                public void setReference(Object object) {
                    this.objects.put(this.index++, object);
                }

                @Override
                public Object findByReference(Integer id) {
                    return this.objects.get(id);
                }
            };
        }
    };

    void setReference(Object object);

    Object findByReference(Integer referenceId);

    @Override
    default void close() {
        INSTANCE.remove();
    }
}
